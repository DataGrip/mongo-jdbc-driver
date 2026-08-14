package com.dbschema.mongo.oidc;

import com.mongodb.MongoCredential;
import com.mongodb.MongoCredential.OidcCallbackContext;
import com.mongodb.MongoCredential.OidcCallbackResult;
import com.nimbusds.oauth2.sdk.Scope;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.RefreshFailedException;

public class OidcCallback implements MongoCredential.OidcCallback {

  private static final Logger logger = Logger.getLogger(OidcCallback.class.getName());

  private final OidcAuthFlow oidcAuthFlow;

  public OidcCallback(@NotNull String redirectHost, @NotNull String redirectPort) {
    this(redirectHost, redirectPort, false);
  }

  public OidcCallback(@NotNull String redirectHost, @NotNull String redirectPort, boolean trustSystemKeychain) {
    logger.log(Level.INFO, "Initializing OIDC callback with redirect: {0}:{1}", new Object[]{redirectHost, redirectPort});
    this.oidcAuthFlow = new OidcAuthFlow(redirectHost, redirectPort, trustSystemKeychain);
  }

  @Override
  public @NotNull OidcCallbackResult onRequest(@NotNull OidcCallbackContext callbackContext) {
    String issuerURI = callbackContext.getIdpInfo() != null
        ? callbackContext.getIdpInfo().getIssuer() : null;
    String clientID = callbackContext.getIdpInfo() != null
        ? callbackContext.getIdpInfo().getClientId() : null;
    // part of the cache key: a token obtained for other scopes is for another audience and must not be reused
    Scope scopes = callbackContext.getIdpInfo() != null
        ? oidcAuthFlow.buildScopes(callbackContext.getIdpInfo()) : null;
    // the principal too: the identity of the connection is the token's, since the server reads it from the
    // token's claims and takes the principal of the handshake only to pick the provider. Sharing an entry
    // between two principals therefore authenticates one of them as the other.
    OidcTokenCache.Key cacheKey =
        OidcTokenCache.Key.of(issuerURI, clientID, scopes, callbackContext.getUserName());

    // 1. Return cached token if still valid
    OidcCallbackResult cached = OidcTokenCache.getIfValid(cacheKey);
    if (cached != null) {
      logger.log(Level.INFO, "Returning cached OIDC token (not expired)");
      return cached;
    }

    // 2. Try to refresh using available refresh tokens
    // Prefer the driver-provided refresh token (most recent), fall back to cached one
    String refreshToken = callbackContext.getRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty()) {
      refreshToken = OidcTokenCache.getRefreshToken(cacheKey);
    }

    if (refreshToken != null && !refreshToken.isEmpty()) {
      logger.log(Level.INFO, "Attempting token refresh");
      OidcCallbackResult refreshed = tryRefresh(callbackContext, refreshToken, cacheKey);
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
                                        OidcTokenCache.Key cacheKey) {
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
    OidcTokenCache.invalidate(cacheKey);
    return null;
  }
}
