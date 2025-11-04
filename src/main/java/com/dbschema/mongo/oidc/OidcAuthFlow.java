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
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.RefreshFailedException;

public class OidcAuthFlow {

    private static final Logger logger = Logger.getLogger(OidcAuthFlow.class.getName());
    private static final String OFFLINE_ACCESS = "offline_access";
    private static final String OPENID = "openid";
    private OidcCallbackResult oidcCallbackResult;

    public OidcAuthFlow() {}

    public Scope buildScopes(
            String clientID, IdpInfo idpServerInfo, OIDCProviderMetadata providerMetadata) {
        Set<String> scopes = new HashSet<>();
        Scope supportedScopes = providerMetadata.getScopes();

        // Add openid and offline_access scopes by default
        scopes.add(OPENID);
        scopes.add(OFFLINE_ACCESS);

        // Add custom scopes from request that are supported by the IdP
        List<String> requestedScopes = idpServerInfo.getRequestScopes();
        if (requestedScopes != null) {
            // azure
            String clientIDDefault = clientID + "/.default";
            if (requestedScopes.contains(clientIDDefault)) {
                scopes.add(clientIDDefault);
            }
            if (supportedScopes != null) {
                for (String scope : requestedScopes) {
                    if (supportedScopes.contains(scope)) {
                        scopes.add(scope);
                    } else {
                        logger.warning(String.format("Scope '%s' is not supported", scope));
                    }
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
        IdpInfo idpServerInfo = callbackContext.getIdpInfo();
        String clientID = idpServerInfo.getClientId();
        String issuerURI = idpServerInfo.getIssuer();

        if (!isValid(idpServerInfo, clientID, issuerURI)) {
            return null;
        }

        Server server = new Server();
        try {
            OIDCProviderMetadata providerMetadata =
                    OIDCProviderMetadata.resolve(new Issuer(issuerURI));
            URI authorizationEndpoint = providerMetadata.getAuthorizationEndpointURI();
            URI tokenEndpoint = providerMetadata.getTokenEndpointURI();
            Scope requestedScopes = buildScopes(clientID, idpServerInfo, providerMetadata);

            server.start();

            URI redirectURI =
                    new URI(
                            "http://localhost:"
                                    + Server.DEFAULT_REDIRECT_PORT
                                    + "/redirect");
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

            try {
                openURL(request.toURI().toString());
            } catch (Exception e) {
                log(Level.SEVERE, "Failed to open the browser: " + e.getMessage());
                return null;
            }

            OidcResponse response = server.getOidcResponse(callbackContext.getTimeout());
            if (response == null || !state.getValue().equals(response.getState())) {
                log(Level.SEVERE, "OIDC response is null or returned an invalid state");
                return null;
            }

            AuthorizationCode code = new AuthorizationCode(response.getCode());
            AuthorizationCodeGrant codeGrant =
                    new AuthorizationCodeGrant(code, redirectURI, codeVerifier);
            TokenRequest tokenRequest =
                    new TokenRequest(tokenEndpoint, new ClientID(clientID), codeGrant);

            HTTPResponse httpResponse = tokenRequest.toHTTPRequest().send();
            TokenResponse tokenResponse = OIDCTokenResponseParser.parse(httpResponse);
            if (!tokenResponse.indicatesSuccess()) {
                log(Level.SEVERE,  String.format("Request failed: %s", httpResponse.getBody()));
                return null;
            }

            return getOidcCallbackResultFromTokenResponse((OIDCTokenResponse) tokenResponse);
        } catch (OidcTimeoutException e) {
            throw e;
        }
        catch (Exception e) {
            log(Level.SEVERE, "Error during OIDC authentication " + e.getMessage());

            return null;
        } finally {
            try {
                Thread.sleep((1000 * 2));
            } catch (InterruptedException e) {
                log(Level.WARNING, "Thread interrupted " + e.getMessage());
            }
            server.stop();
            switchContext();
        }
    }

    private void switchContext() {
        String osName = System.getProperty("os.name").toLowerCase();
        logger.log(Level.INFO, String.format("osName: %s", osName));
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec(new String[]{"osascript", "-e" , "tell application \"Datagrip\" to activate"});
        } catch (IOException e) {
            log(Level.SEVERE,e.getMessage());
        }

    }

    /**
     * Opens the specified URI in the default web browser, supporting macOS, Windows, and
     * Linux/Unix. This method uses platform-specific commands to invoke the browser.
     *
     * @param url the URL to be opened as a string
     * @throws Exception if no supported browser is found or an error occurs while attempting to
     *     open the URL
     */
    private void openURL(String url) throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        logger.log(Level.INFO, String.format("osName: %s", osName));
        Runtime runtime = Runtime.getRuntime();

        if (osName.contains("windows")) {
            runtime.exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", url});
        } else if (osName.contains("mac os")) {
            runtime.exec(new String[] {"open", "-gj" ,url});
        } else {
            String[] browsers = {"xdg-open", "firefox", "google-chrome"};
            IOException lastError = null;
            for (String browser : browsers) {
                try {
                    // Check if browser exists
                    Process process = runtime.exec(new String[] {"which", browser});
                    if (process.waitFor() == 0) {
                        runtime.exec(new String[] {browser, url});
                    }
                } catch (IOException e) {
                    lastError = e;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            throw lastError != null
                    ? lastError
                    : new IOException("No web browser found to open the URL");
        }
    }

    private void log(Level level, String message) {
        logger.log(level, message);
    }

    public OidcCallbackResult doRefresh(OidcCallbackContext callbackContext)
            throws RefreshFailedException {
        IdpInfo idpServerInfo = callbackContext.getIdpInfo();
        String clientID = idpServerInfo.getClientId();
        String issuerURI = idpServerInfo.getIssuer();

        // Check that the IdP information is valid
        if (!isValid(idpServerInfo, clientID, issuerURI)) {
            return null;
        }
        try {
            // Use OpenID Connect Discovery to fetch the provider metadata
            OIDCProviderMetadata providerMetadata =
                    OIDCProviderMetadata.resolve(new Issuer(issuerURI));
            URI tokenEndpoint = providerMetadata.getTokenEndpointURI();

            // This function will never be called without a refresh token (to be checked in the driver function),
            // but we throw an exception to be explicit about the fact that we expect a refresh token.
            String refreshToken = callbackContext.getRefreshToken();
            if (refreshToken == null) {
                throw new IllegalArgumentException("Refresh token is required");
            }

            RefreshTokenGrant refreshTokenGrant =
                    new RefreshTokenGrant(new RefreshToken(refreshToken));
            TokenRequest tokenRequest =
                    new TokenRequest(tokenEndpoint, new ClientID(clientID), refreshTokenGrant);
            HTTPResponse httpResponse = tokenRequest.toHTTPRequest().send();

            try {
                TokenResponse tokenResponse = OIDCTokenResponseParser.parse(httpResponse);
                if (!tokenResponse.indicatesSuccess()) {
                    TokenErrorResponse errorResponse = tokenResponse.toErrorResponse();
                    String errorCode =
                            errorResponse.getErrorObject() != null
                                    ? errorResponse.getErrorObject().getCode()
                                    : null;
                    String errorDescription =
                            errorResponse.getErrorObject() != null
                                    ? errorResponse.getErrorObject().getDescription()
                                    : null;
                    throw new RefreshFailedException(
                            "Token refresh failed with error: "
                                    + "code="
                                    + errorCode
                                    + ", description="
                                    + errorDescription);
                }
                return getOidcCallbackResultFromTokenResponse((OIDCTokenResponse) tokenResponse);
            } catch (ParseException e) {
                throw new RefreshFailedException(
                        "Failed to parse server response: "
                                + e.getMessage()
                                + " [response="
                                + httpResponse.getBody()
                                + "]");
            }

        } catch (Exception e) {
            log(Level.SEVERE, "OpenID Connect: Error during token refresh. " + e.getMessage());
            if (e instanceof RefreshFailedException) {
                throw (RefreshFailedException) e;
            }
            return null;
        }
    }

    private boolean isValid(IdpInfo idpInfo, String clientID, String issuerURI) {
        return idpInfo != null && clientID != null && !clientID.isEmpty() && issuerURI != null;
    }

    private OidcCallbackResult getOidcCallbackResultFromTokenResponse(
            OIDCTokenResponse tokenResponse) {
        Tokens tokens = tokenResponse.getOIDCTokens();
        String accessToken = tokens.getAccessToken().getValue();
        String refreshToken =
                tokens.getRefreshToken() != null ? tokens.getRefreshToken().getValue() : null;
        Duration expiresIn = Duration.ofSeconds(tokens.getAccessToken().getLifetime());

        this.oidcCallbackResult = new OidcCallbackResult(accessToken, expiresIn, refreshToken);

        return this.oidcCallbackResult;
    }
}
