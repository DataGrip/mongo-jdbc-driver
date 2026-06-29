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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
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

  public OidcAuthFlow(@NotNull String redirectHost, @NotNull String redirectPort) {
    this.redirectHost = redirectHost;
    this.redirectPort = redirectPort;
  }

  public Scope buildScopes(String clientID, IdpInfo idpServerInfo, OIDCProviderMetadata providerMetadata) {
    Set<String> scopes = new HashSet<>();
    Scope supportedScopes = providerMetadata.getScopes();

    scopes.add(OPENID);
    scopes.add(OFFLINE_ACCESS);

    List<String> requestedScopes = idpServerInfo.getRequestScopes();
    String clientIDDefault = clientID + "/.default";
    if (requestedScopes.contains(clientIDDefault)) {
      scopes.add(clientIDDefault);
    }
    if (supportedScopes != null) {
      for (String scope : requestedScopes) {
        if (supportedScopes.contains(scope)) {
          scopes.add(scope);
        }
        else {
          logger.warning(String.format("Scope '%s' is not supported", scope));
        }
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

    Server server = new Server(this.redirectHost, this.redirectPort);
    try {
      OIDCProviderMetadata providerMetadata =
          OIDCProviderMetadata.resolve(new Issuer(issuerURI));
      URI authorizationEndpoint = providerMetadata.getAuthorizationEndpointURI();
      URI tokenEndpoint = providerMetadata.getTokenEndpointURI();
      Scope requestedScopes = buildScopes(clientID, idpServerInfo, providerMetadata);

      server.start();

      URI redirectURI = new URI("http", null, server.getRedirectHost(), server.getPort(), REDIRECT_ENDPOINT, null, null);
      State state = new State();
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
      if (response == null || !state.getValue().equals(response.getState())) {
        throw new IllegalStateException("OIDC response is null or returned an invalid state");
      }

      AuthorizationCode code = new AuthorizationCode(response.getCode());
      AuthorizationCodeGrant codeGrant =
          new AuthorizationCodeGrant(code, redirectURI, codeVerifier);
      TokenRequest.Builder tokenRequestBuilder = new TokenRequest.Builder(tokenEndpoint, new ClientID(clientID), codeGrant);
      TokenRequest tokenRequest = tokenRequestBuilder.build();

      HTTPResponse httpResponse = tokenRequest.toHTTPRequest().send();
      TokenResponse tokenResponse = OIDCTokenResponseParser.parse(httpResponse);
      if (!tokenResponse.indicatesSuccess()) {
        throw new IllegalStateException(String.format("Token request failed: %s", httpResponse.getBody()));
      }

      return buildCallbackResult((OIDCTokenResponse) tokenResponse, issuerURI, clientID, null);
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
      OIDCProviderMetadata providerMetadata =
          OIDCProviderMetadata.resolve(new Issuer(issuerURI));
      URI tokenEndpoint = providerMetadata.getTokenEndpointURI();

      if (refreshTokenValue == null) {
        throw new IllegalArgumentException("Refresh token is required");
      }

      RefreshTokenGrant refreshTokenGrant = new RefreshTokenGrant(new RefreshToken(refreshTokenValue));

      TokenRequest.Builder tokenRequestBuilder = new TokenRequest.Builder(tokenEndpoint, new ClientID(clientID), refreshTokenGrant);
      TokenRequest tokenRequest = tokenRequestBuilder.build();

      HTTPResponse httpResponse = tokenRequest.toHTTPRequest().send();

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
        return buildCallbackResult((OIDCTokenResponse) tokenResponse, issuerURI, clientID, refreshTokenValue);
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

  private boolean isValid(IdpInfo idpInfo, String clientID, String issuerURI) {
    return idpInfo != null && clientID != null && !clientID.isEmpty() && issuerURI != null;
  }

  private OidcCallbackResult buildCallbackResult(
      OIDCTokenResponse tokenResponse, String issuerURI, String clientID, String fallbackRefreshToken) {
    Tokens tokens = tokenResponse.getOIDCTokens();
    String accessToken = tokens.getAccessToken().getValue();
    String refreshToken =
        tokens.getRefreshToken() != null ? tokens.getRefreshToken().getValue() : fallbackRefreshToken;
    Duration expiresIn = Duration.ofSeconds(tokens.getAccessToken().getLifetime());

    OidcCallbackResult result = new OidcCallbackResult(accessToken, expiresIn, refreshToken);
    OidcTokenCache.put(issuerURI, clientID, result, expiresIn);
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
