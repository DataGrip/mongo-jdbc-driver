package com.dbschema.mongo.oidc;

import com.mongodb.MongoCredential.OidcCallbackResult;
import com.nimbusds.oauth2.sdk.Scope;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process-wide cache for OIDC tokens, keyed by issuer URI, client ID, requested scopes and principal
 * ({@link Key}). Avoids re-prompting the user through the browser-based auth code flow
 * every time a new JDBC connection is opened. Cached tokens are reused as long
 * as they have not expired; when they expire, a refresh is attempted before
 * falling back to the full interactive flow.
 *
 * Thread-safe: all operations use {@link ConcurrentHashMap}.
 */
public final class OidcTokenCache {

    private static final Logger logger = Logger.getLogger(OidcTokenCache.class.getName());
    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(30);
    private static final ConcurrentHashMap<Key, CachedToken> cache = new ConcurrentHashMap<>();

    private OidcTokenCache() {
    }

    /**
     * Everything that makes a cached token belong to one connection rather than another. Built through
     * {@link #of} by both the reader and the writer of the cache, so that no component of it can be
     * left out on one side only.
     */
    public record Key(String issuerURI, String clientID, String scopes, String principal) {

        /**
         * @param principal kept as it is, {@code null} included. A connection sending no principal is a
         *                  configuration of its own and does not share a token with one that sends any --
         *                  but it does share with every other connection sending none: there is nothing
         *                  left to tell them apart by. Two identities at one provider therefore need
         *                  {@code oidcPrincipal} set, since with {@code oidcPrincipal=none} the second
         *                  connection reuses the token the first one obtained. Not case-folded either --
         *                  two spellings of one address cost an extra browser login, while folding them
         *                  would be a guess about how the provider compares identities.
         */
        public static Key of(String issuerURI, String clientID, Scope scopes, String principal) {
            // Scope is a mutable LinkedHashSet, so its text is what gets held as part of a map key
            return new Key(issuerURI, clientID, scopes == null ? "" : scopes.toString(), principal);
        }

        boolean identifiesAProvider() {
            return issuerURI != null && clientID != null;
        }
    }

    /**
     * Stores a callback result in the cache along with its expiry duration.
     * The {@code expiresIn} parameter is required because {@link OidcCallbackResult}
     * does not expose a getter for the expiry duration.
     */
    public static void put(Key key, OidcCallbackResult result, Duration expiresIn) {
        if (key == null || !key.identifiesAProvider() || result == null) return;
        cache.put(key, new CachedToken(result, Instant.now(), expiresIn));
        logger.log(Level.FINE, "Cached OIDC token for {0}", key);
    }

    /**
     * Returns a cached result if it exists and has not yet expired.
     */
    public static OidcCallbackResult getIfValid(Key key) {
        if (key == null || !key.identifiesAProvider()) return null;
        CachedToken entry = cache.get(key);
        if (entry == null) return null;
        if (entry.isExpired()) {
            logger.log(Level.FINE, "Cached OIDC token for {0} has expired", key);
            return null;
        }
        return entry.result;
    }

    /**
     * Returns the refresh token from a cached entry, regardless of whether the
     * access token has expired. Useful for performing a token refresh.
     */
    public static String getRefreshToken(Key key) {
        if (key == null || !key.identifiesAProvider()) return null;
        CachedToken entry = cache.get(key);
        if (entry == null || entry.result == null) return null;
        return entry.result.getRefreshToken();
    }

    /**
     * Removes a cached entry, e.g. after a failed refresh.
     */
    public static void invalidate(Key key) {
        if (key == null || !key.identifiesAProvider()) return;
        cache.remove(key);
        logger.log(Level.FINE, "Invalidated cached OIDC token for {0}", key);
    }

    private record CachedToken(OidcCallbackResult result, Instant storedAt, Duration expiresIn) {

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
