package com.dbschema.mongo.oidc;

import com.mongodb.MongoCredential.IdpInfo;
import com.mongodb.MongoCredential.OidcCallbackContext;
import com.mongodb.MongoCredential.OidcCallbackResult;
import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.ResponseType;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPRequestConfigurator;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse;
import com.nimbusds.openid.connect.sdk.OIDCTokenResponseParser;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLSocketFactory;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.RefreshFailedException;

import static com.dbschema.mongo.oidc.Server.REDIRECT_ENDPOINT;

public class OidcAuthFlow {

  private static final Logger logger = Logger.getLogger(OidcAuthFlow.class.getName());
  private static final String OFFLINE_ACCESS = "offline_access";
  private static final String OPENID = "openid";
  private final String redirectPort;
  private final String redirectHost;
  private final boolean trustSystemKeychain;

  public OidcAuthFlow(@NotNull String redirectHost, @NotNull String redirectPort) {
    this(redirectHost, redirectPort, false);
  }

  public OidcAuthFlow(@NotNull String redirectHost, @NotNull String redirectPort, boolean trustSystemKeychain) {
    this.redirectHost = redirectHost;
    this.redirectPort = redirectPort;
    this.trustSystemKeychain = trustSystemKeychain;
  }

  /**
   * The scopes to request: the OIDC ones this flow needs, plus every scope the server asked for. They are
   * not filtered against the provider's advertised 'scopes_supported' -- a resource scope is routinely
   * absent from it, and dropping it makes the provider issue a token for the wrong audience.
   */
  @NotNull
  public Scope buildScopes(@NotNull IdpInfo idpServerInfo) {
    // ordered, so the requested scopes and the token cache key are deterministic
    Set<String> scopes = new LinkedHashSet<>();
    scopes.add(OPENID);
    scopes.add(OFFLINE_ACCESS);

    List<String> requested = idpServerInfo.getRequestScopes();

    for (String scope : requested) {
      if (scope != null && !scope.trim().isEmpty()) {
        scopes.add(scope.trim());
      }
    }

    Scope finalScopes = new Scope();
    for (String scope : scopes) {
      finalScopes.add(new Scope.Value(scope));
    }
    return finalScopes;
  }

  public OidcCallbackResult doAuthCodeFlow(OidcCallbackContext callbackContext)
      throws OidcTimeoutException {

    if(callbackContext.getIdpInfo() == null) {
      throw new IllegalStateException("OIDC configuration is incomplete: missing IdpInfo");
    }

    IdpInfo idpServerInfo = callbackContext.getIdpInfo();
    String clientID = idpServerInfo.getClientId();
    String issuerURI = idpServerInfo.getIssuer();

    if (!isValid(idpServerInfo, clientID, issuerURI)) {
      throw new IllegalStateException("OIDC configuration is incomplete: missing IdpInfo, clientID, or issuerURI");
    }

    // created before the server, which refuses every callback that does not carry this exact value
    State state = new State();
    Server server = new Server(this.redirectHost, this.redirectPort, state.getValue());
    try {
      SSLSocketFactory sslSocketFactory = systemSocketFactory();
      OIDCProviderMetadata providerMetadata =
          OIDCProviderMetadata.resolve(new Issuer(issuerURI), httpRequestConfigurator(sslSocketFactory));
      URI authorizationEndpoint = providerMetadata.getAuthorizationEndpointURI();
      URI tokenEndpoint = providerMetadata.getTokenEndpointURI();
      Scope requestedScopes = buildScopes(idpServerInfo);
      logger.log(Level.INFO, "Requesting OIDC scopes: {0}", requestedScopes);

      server.start();

      URI redirectURI = new URI("http", null, server.getRedirectHost(), server.getPort(), REDIRECT_ENDPOINT, null, null);
      CodeVerifier codeVerifier = new CodeVerifier();

      AuthorizationRequest request =
          new AuthorizationRequest.Builder(
                  new ResponseType(ResponseType.Value.CODE),
                  new ClientID(clientID))
              .scope(requestedScopes)
              .redirectionURI(redirectURI)
              .state(state)
              .codeChallenge(codeVerifier, CodeChallengeMethod.S256)
              .endpointURI(authorizationEndpoint)
              .build();

      URI authorizationURI = request.toURI();
      if (authorizationURI == null) {
        throw new IllegalStateException("Authorization request URI is null");
      }

      try {
        openURL(authorizationURI);
      }
      catch (Exception e) {
        throw new IllegalStateException("Failed to open the browser: " + e.getMessage(), e);
      }

      OidcResponse response = server.getOidcResponse(callbackContext.getTimeout());
      if (response == null) {
        throw new IllegalStateException("OIDC response is null");
      }
      // reported as the error it is: 'invalid state' used to cover this case too, since an error response
      // carried no state and so could not tell the two apart
      if (response.getError() != null) {
        throw new IllegalStateException(String.format("OIDC authorization failed: %s (%s)",
            response.getError(), response.getErrorDescription()));
      }
      // the server already refuses a callback with any other state; kept as defence in depth
      if (!state.getValue().equals(response.getState())) {
        throw new IllegalStateException("OIDC response returned an invalid state");
      }

      AuthorizationCode code = new AuthorizationCode(response.getCode());
      AuthorizationCodeGrant codeGrant =
          new AuthorizationCodeGrant(code, redirectURI, codeVerifier);
      TokenRequest tokenRequest = new TokenRequest.Builder(tokenEndpoint, new ClientID(clientID), codeGrant).build();

      HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
      if (sslSocketFactory != null) {
        httpRequest.setSSLSocketFactory(sslSocketFactory);
      }
      HTTPResponse httpResponse = httpRequest.send();
      TokenResponse tokenResponse = OIDCTokenResponseParser.parse(httpResponse);
      if (!tokenResponse.indicatesSuccess()) {
        throw new IllegalStateException(String.format("Token request failed: %s", httpResponse.getBody()));
      }

      return buildCallbackResult((OIDCTokenResponse) tokenResponse, issuerURI, clientID, requestedScopes,
          callbackContext.getUserName(), null);
    }
    catch (OidcTimeoutException e) {
      throw e;
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("OIDC authentication interrupted", e);
    }
    catch (Exception e) {
      throw new RuntimeException("Error during OIDC authentication: " + e.getMessage(), e);
    }
    finally {
      server.stop();
    }
  }

  public OidcCallbackResult doRefresh(OidcCallbackContext callbackContext, String refreshTokenValue)
      throws RefreshFailedException {

    if(callbackContext.getIdpInfo() == null) {
      throw new IllegalStateException("OIDC configuration is incomplete: missing IdpInfo");
    }

    IdpInfo idpServerInfo = callbackContext.getIdpInfo();
    String clientID = idpServerInfo.getClientId();
    String issuerURI = idpServerInfo.getIssuer();

    if (!isValid(idpServerInfo, clientID, issuerURI)) {
      return null;
    }
    try {
      SSLSocketFactory sslSocketFactory = systemSocketFactory();
      OIDCProviderMetadata providerMetadata =
          OIDCProviderMetadata.resolve(new Issuer(issuerURI), httpRequestConfigurator(sslSocketFactory));
      URI tokenEndpoint = providerMetadata.getTokenEndpointURI();

      if (refreshTokenValue == null) {
        throw new IllegalArgumentException("Refresh token is required");
      }

      // No 'scope' is sent: per RFC 6749 section 6 an omitted scope means the refreshed token keeps the
      // originally granted scope, which is the same-audience guarantee we need. Echoing the requested
      // scopes instead would fail with 'invalid_scope' whenever the provider granted only a subset of
      // them, because the refresh scope must not contain anything that was never granted.
      // requestedScopes is still needed as the token cache key, and must match the key OidcCallback builds.
      Scope requestedScopes = buildScopes(idpServerInfo);
      logger.log(Level.FINE, "Refreshing OIDC token cached for scopes: {0}", requestedScopes);
      TokenRequest tokenRequest = buildRefreshTokenRequest(tokenEndpoint, clientID, refreshTokenValue);

      HTTPRequest httpRequest = tokenRequest.toHTTPRequest();
      if (sslSocketFactory != null) {
        httpRequest.setSSLSocketFactory(sslSocketFactory);
      }
      HTTPResponse httpResponse = httpRequest.send();

      try {
        TokenResponse tokenResponse = OIDCTokenResponseParser.parse(httpResponse);
        if (!tokenResponse.indicatesSuccess()) {
          TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
          String errorCode = errorResponse.getErrorObject() != null
              ? errorResponse.getErrorObject().getCode() : null;
          String errorDescription = errorResponse.getErrorObject() != null
              ? errorResponse.getErrorObject().getDescription() : null;
          throw new RefreshFailedException(
              "Token refresh failed: code=" + errorCode + ", description=" + errorDescription);
        }
        return buildCallbackResult((OIDCTokenResponse) tokenResponse, issuerURI, clientID, requestedScopes,
            callbackContext.getUserName(), refreshTokenValue);
      }
      catch (ParseException e) {
        throw new RefreshFailedException(
            "Failed to parse server response: " + e.getMessage()
                + " [response=" + httpResponse.getBody() + "]");
      }
    }
    catch (RefreshFailedException e) {
      throw e;
    }
    catch (Exception e) {
      logger.log(Level.SEVERE, "OpenID Connect: Error during token refresh. " + e.getMessage());
      throw new RefreshFailedException("Token refresh failed: " + e.getMessage());
    }
  }

  private static void logNotGrantedScopes(@Nullable Scope requested, @Nullable Scope granted) {
    if (requested == null || granted == null || granted.isEmpty()) return;
    Scope notGranted = new Scope();
    for (Scope.Value value : requested) {
      if (!granted.contains(value)) {
        notGranted.add(value);
      }
    }
    if (!notGranted.isEmpty()) {
      logger.log(Level.WARNING, "Requested OIDC scopes not granted by the provider: {0}, granted: {1}",
          new Object[]{notGranted, granted});
    }
  }

  @NotNull
  public static TokenRequest buildRefreshTokenRequest(@NotNull URI tokenEndpoint, @NotNull String clientID,
                                                      @NotNull String refreshTokenValue) {
    RefreshTokenGrant refreshTokenGrant = new RefreshTokenGrant(new RefreshToken(refreshTokenValue));
    return new TokenRequest.Builder(tokenEndpoint, new ClientID(clientID), refreshTokenGrant).build();
  }

  private boolean isValid(IdpInfo idpInfo, String clientID, String issuerURI) {
    return idpInfo != null && clientID != null && !clientID.isEmpty() && issuerURI != null;
  }

  /**
   * Builds an {@link SSLSocketFactory} trusting the system certificate stores, used for the outgoing
   * OIDC HTTPS calls so they succeed behind a corporate / custom root CA. Returns {@code null} when
   * the factory cannot be built, in which case the SDK defaults are used.
   */
  private SSLSocketFactory systemSocketFactory() {
    try {
      return OidcTls.systemSocketFactory(trustSystemKeychain);
    }
    catch (GeneralSecurityException e) {
      logger.log(Level.WARNING, "Falling back to default TLS trust settings: " + e.getMessage());
      return null;
    }
  }

  private HTTPRequestConfigurator httpRequestConfigurator(SSLSocketFactory sslSocketFactory) {
    return httpRequest -> {
      if (sslSocketFactory != null) {
        httpRequest.setSSLSocketFactory(sslSocketFactory);
      }
    };
  }

  /**
   * @param principal the principal of the connection this token was obtained for, part of the cache key:
   *                  see {@link OidcTokenCache.Key}
   */
  private OidcCallbackResult buildCallbackResult(
      OIDCTokenResponse tokenResponse, String issuerURI, String clientID, Scope scopes, String principal,
      String fallbackRefreshToken) {
    Tokens tokens = tokenResponse.getOIDCTokens();
    String accessToken = tokens.getAccessToken().getValue();
    String refreshToken =
        tokens.getRefreshToken() != null ? tokens.getRefreshToken().getValue() : fallbackRefreshToken;
    Duration expiresIn = Duration.ofSeconds(tokens.getAccessToken().getLifetime());
    logNotGrantedScopes(scopes, tokens.getAccessToken().getScope());

    OidcCallbackResult result = new OidcCallbackResult(accessToken, expiresIn, refreshToken);
    OidcTokenCache.put(OidcTokenCache.Key.of(issuerURI, clientID, scopes, principal), result, expiresIn);
    return result;
  }

  private static final int MAX_URI_LENGTH = 2048;
  private static final Set<String> ALLOWED_SCHEMES = Set.of("https", "http");

  /**
   * Opens the specified URI in the default web browser.
   *
   * Tries {@link Desktop#browse(URI)} first. When running inside DataGrip's
   * driver JVM the custom AWT toolkit's {@code isSupported()} probe calls
   * {@code browse(null)}, which crashes on the IDE side. In that case we
   * fall back to platform commands matching IntelliJ's own
   * {@code BrowserLauncherAppless}.
   */
  private void openURL(URI uri) throws IOException {
    validateURI(uri);

    try {
      Desktop.getDesktop().browse(uri);
      return;
    }
    catch (Exception e) {
      logger.log(Level.WARNING, "Desktop.browse() failed, falling back to platform command", e);
    }

    ProcessBuilder pb = new ProcessBuilder(buildBrowserOpenCommand(System.getProperty("os.name", ""), uri));
    pb.redirectErrorStream(true);
    pb.start();
  }

  static List<String> buildBrowserOpenCommand(String osName, URI uri) {
    String normalizedOsName = osName == null ? "" : osName.toLowerCase();
    if (normalizedOsName.contains("mac")) {
      return List.of("open", uri.toString());
    }
    else if (normalizedOsName.contains("windows")) {
      return List.of("rundll32.exe", "url.dll,FileProtocolHandler", uri.toString());
    }
    else if (normalizedOsName.contains("linux")) {
      return List.of("xdg-open", uri.toString());
    }
    else {
      throw new UnsupportedOperationException("Cannot open browser on " + osName);
    }
  }

  private static void validateURI(URI uri) {
    String scheme = uri.getScheme();
    if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
      throw new IllegalArgumentException("Refusing to open URI with scheme: " + scheme);
    }

    String host = uri.getHost();
    if (host == null || host.isEmpty()) {
      throw new IllegalArgumentException("URI must have a host");
    }

    if (uri.toString().length() > MAX_URI_LENGTH) {
      throw new IllegalArgumentException("URI exceeds maximum length of " + MAX_URI_LENGTH);
    }

    if (uri.getUserInfo() != null) {
      throw new IllegalArgumentException("URI must not contain user info");
    }

    String uriString = uri.toString();
    if (uriString.contains("..") || uriString.contains("\n") || uriString.contains("\r")) {
      throw new IllegalArgumentException("URI contains invalid characters");
    }
  }
}
