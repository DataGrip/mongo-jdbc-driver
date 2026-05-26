package com.dbschema.mongo.oidc;

import com.mongodb.MongoCredential.OidcCallbackResult;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process-wide cache for OIDC tokens, keyed by issuer URI and client ID.
 * Avoids re-prompting the user through the browser-based auth code flow
 * every time a new JDBC connection is opened. Cached tokens are reused as long
 * as they have not expired; when they expire, a refresh is attempted before
 * falling back to the full interactive flow.
 *
 * Thread-safe: all operations use {@link ConcurrentHashMap}.
 */
public final class OidcTokenCache {

    private static final Logger logger = Logger.getLogger(OidcTokenCache.class.getName());
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);
    private static final ConcurrentHashMap<String, CachedToken> cache = new ConcurrentHashMap<>();

    private OidcTokenCache() {
    }

    /**
     * Stores a callback result in the cache along with its expiry duration.
     * The {@code expiresIn} parameter is required because {@link OidcCallbackResult}
     * does not expose a getter for the expiry duration.
     */
    public static void put(String issuerURI, String clientID, OidcCallbackResult result, Duration expiresIn) {
        if (issuerURI == null || clientID == null || result == null) return;
        String key = buildKey(issuerURI, clientID);
        cache.put(key, new CachedToken(result, Instant.now(), expiresIn));
        logger.log(Level.FINE, "Cached OIDC token for {0}", key);
    }

    /**
     * Returns a cached result if it exists and has not yet expired.
     */
    public static OidcCallbackResult getIfValid(String issuerURI, String clientID) {
        if (issuerURI == null || clientID == null) return null;
        CachedToken entry = cache.get(buildKey(issuerURI, clientID));
        if (entry == null) return null;
        if (entry.isExpired()) {
            logger.log(Level.FINE, "Cached OIDC token for {0} has expired", buildKey(issuerURI, clientID));
            return null;
        }
        return entry.result;
    }

    /**
     * Returns the refresh token from a cached entry, regardless of whether the
     * access token has expired. Useful for performing a token refresh.
     */
    public static String getRefreshToken(String issuerURI, String clientID) {
        if (issuerURI == null || clientID == null) return null;
        CachedToken entry = cache.get(buildKey(issuerURI, clientID));
        if (entry == null || entry.result == null) return null;
        return entry.result.getRefreshToken();
    }

    /**
     * Removes a cached entry, e.g. after a failed refresh.
     */
    public static void invalidate(String issuerURI, String clientID) {
        if (issuerURI == null || clientID == null) return;
        String key = buildKey(issuerURI, clientID);
        cache.remove(key);
        logger.log(Level.FINE, "Invalidated cached OIDC token for {0}", key);
    }

    private static String buildKey(String issuerURI, String clientID) {
        return issuerURI + "|" + clientID;
    }

    private static final class CachedToken {
        final OidcCallbackResult result;
        final Instant storedAt;
        final Duration expiresIn;

        CachedToken(OidcCallbackResult result, Instant storedAt, Duration expiresIn) {
            this.result = result;
            this.storedAt = storedAt;
            this.expiresIn = expiresIn;
        }

        boolean isExpired() {
            if (expiresIn == null) {
                return true;
            }
            Duration effectiveTtl = expiresIn.minus(EXPIRY_MARGIN);
            if (effectiveTtl.isNegative() || effectiveTtl.isZero()) {
                return true;
            }
            return Instant.now().isAfter(storedAt.plus(effectiveTtl));
        }
    }
}
