package com.dbschema.mongo.oidc;

import com.mongodb.MongoCredential;
import com.mongodb.MongoCredential.OidcCallbackContext;
import com.mongodb.MongoCredential.OidcCallbackResult;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.RefreshFailedException;

public class OidcCallback implements MongoCredential.OidcCallback {

    private static final Logger logger = Logger.getLogger(OidcCallback.class.getName());

    private final OidcAuthFlow oidcAuthFlow;

    public OidcCallback() {
        this.oidcAuthFlow = new OidcAuthFlow();
    }

    public OidcAuthFlow getOidcAuthFlow() {
        return this.oidcAuthFlow;
    }

    @Override
    public OidcCallbackResult onRequest(OidcCallbackContext callbackContext) {
        String issuerURI = callbackContext.getIdpInfo() != null
                ? callbackContext.getIdpInfo().getIssuer() : null;
        String clientID = callbackContext.getIdpInfo() != null
                ? callbackContext.getIdpInfo().getClientId() : null;

        // 1. Return cached token if still valid
        OidcCallbackResult cached = OidcTokenCache.getIfValid(issuerURI, clientID);
        if (cached != null) {
            logger.log(Level.INFO, "Returning cached OIDC token (not expired)");
            return cached;
        }

        // 2. Try to refresh using available refresh tokens
        // Prefer the driver-provided refresh token (most recent), fall back to cached one
        String refreshToken = callbackContext.getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            refreshToken = OidcTokenCache.getRefreshToken(issuerURI, clientID);
        }

        if (refreshToken != null && !refreshToken.isEmpty()) {
            logger.log(Level.INFO, "Attempting token refresh");
            OidcCallbackResult refreshed = tryRefresh(callbackContext, refreshToken, issuerURI, clientID);
            if (refreshed != null) {
                return refreshed;
            }
        }

        // 3. Fall back to full browser-based auth code flow
        logger.log(Level.INFO, "Performing full OIDC auth code flow");
        try {
            return oidcAuthFlow.doAuthCodeFlow(callbackContext);
        }
        catch (OidcTimeoutException e) {
            logger.log(Level.SEVERE, "OIDC auth code flow timed out", e);
            throw new RuntimeException(e);
        }
    }

    private OidcCallbackResult tryRefresh(OidcCallbackContext callbackContext,
                                          String refreshToken,
                                          String issuerURI, String clientID) {
        try {
            OidcCallbackResult refreshed = oidcAuthFlow.doRefresh(callbackContext, refreshToken);
            if (refreshed != null) {
                return refreshed;
            }
            logger.log(Level.WARNING, "Token refresh returned null, invalidating cache");
        }
        catch (RefreshFailedException e) {
            logger.log(Level.WARNING, "Token refresh failed: " + e.getMessage());
        }
        OidcTokenCache.invalidate(issuerURI, clientID);
        return null;
    }
}
