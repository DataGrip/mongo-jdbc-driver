package com.dbschema.mongo;

import com.dbschema.mongo.oidc.OidcTokenCache;
import com.dbschema.mongo.oidc.OidcTokenCache.Key;
import com.mongodb.MongoCredential.OidcCallbackResult;
import com.nimbusds.oauth2.sdk.Scope;
import org.junit.After;
import org.junit.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class OidcTokenCacheTest {
    private static final String ISSUER = "https://login.microsoftonline.com/tenant/v2.0";
    private static final String CLIENT_ID = "client";
    private static final String ALICE = "alice@mycorp.net";
    private static final String ADMIN = "admin@mycorp.net";
    private static final Duration HOUR = Duration.ofHours(1);

    private final List<Key> keys = new ArrayList<>();

    /**
     * The cache is process-wide and has no reset, so every key a test builds is dropped here rather than at
     * the end of the test body: a failing assertion would otherwise leave the entry behind and let one real
     * failure cascade into the tests that follow.
     */
    @After
    public void clearCachedTokens() {
        keys.forEach(OidcTokenCache::invalidate);
    }

    /**
     * The requested scopes decide the audience of the token, so a token obtained for another scope set must
     * not be served from the cache: otherwise a wrong-audience token keeps being reused until it expires.
     */
    @Test
    public void tokensOfDifferentScopesDoNotShareAnEntry() {
        Key requested = key(scope("openid", "api://abc/.default"), ALICE);
        Key other = key(scope("openid"), ALICE);
        OidcTokenCache.put(requested, result("for-requested"), HOUR);

        assertEquals("for-requested", OidcTokenCache.getIfValid(requested).getAccessToken());
        assertNull(OidcTokenCache.getIfValid(other));
        assertNull(OidcTokenCache.getRefreshToken(other));
    }

    /**
     * The cache is process-wide, so two data sources of one IDE against the same server used to share an
     * entry whenever issuer, client ID and scopes matched -- whatever principal each of them was for. The
     * second connection was then handed the first user's access and refresh tokens and authenticated as
     * that user, silently and with their privileges: the server reads the identity from the claims of the
     * token, and takes the principal of the handshake only to select the identity provider.
     */
    @Test
    public void tokensOfDifferentPrincipalsDoNotShareAnEntry() {
        Scope scopes = scope("openid", "api://abc/.default");
        Key alice = key(scopes, ALICE);
        Key admin = key(scopes, ADMIN);
        OidcTokenCache.put(alice, result("alices-token"), HOUR);

        assertNull(OidcTokenCache.getIfValid(admin));
        assertNull(OidcTokenCache.getRefreshToken(admin));
        assertEquals("alices-token", OidcTokenCache.getIfValid(alice).getAccessToken());
    }

    /** A connection sending no principal at all ('oidcPrincipal=none') is a configuration of its own. */
    @Test
    public void aSuppressedPrincipalIsItsOwnEntry() {
        Scope scopes = scope("openid", "suppressed");
        Key none = key(scopes, null);
        Key alice = key(scopes, ALICE);

        OidcTokenCache.put(none, result("without-principal"), HOUR);
        assertNull(OidcTokenCache.getIfValid(alice));

        OidcTokenCache.put(alice, result("for-alice"), HOUR);
        assertEquals("without-principal", OidcTokenCache.getIfValid(none).getAccessToken());
        assertEquals("for-alice", OidcTokenCache.getIfValid(alice).getAccessToken());
    }

    @Test
    public void invalidateOnlyRemovesTheGivenScopes() {
        Key kept = key(scope("openid", "keep"), ALICE);
        Key removed = key(scope("openid", "remove"), ALICE);
        OidcTokenCache.put(kept, result("kept"), HOUR);
        OidcTokenCache.put(removed, result("removed"), HOUR);

        OidcTokenCache.invalidate(removed);

        assertNull(OidcTokenCache.getIfValid(removed));
        assertNotNull(OidcTokenCache.getIfValid(kept));
    }

    /** A refresh that failed for one principal must not push the other through a new browser login. */
    @Test
    public void invalidateOnlyRemovesTheGivenPrincipal() {
        Scope scopes = scope("openid", "shared");
        Key alice = key(scopes, ALICE);
        Key admin = key(scopes, ADMIN);
        OidcTokenCache.put(alice, result("alices-token"), HOUR);
        OidcTokenCache.put(admin, result("admins-token"), HOUR);

        OidcTokenCache.invalidate(admin);

        assertNull(OidcTokenCache.getIfValid(admin));
        assertEquals("alices-token", OidcTokenCache.getIfValid(alice).getAccessToken());
    }

    @Test
    public void expiredTokenIsNotReturnedButItsRefreshTokenIs() {
        Key expired = key(scope("openid", "expired"), ALICE);
        OidcTokenCache.put(expired, result("stale"), Duration.ofSeconds(1));

        assertNull(OidcTokenCache.getIfValid(expired));
        assertEquals("refresh", OidcTokenCache.getRefreshToken(expired));
    }

    private Key key(Scope scopes, String principal) {
        Key key = Key.of(ISSUER, CLIENT_ID, scopes, principal);
        keys.add(key);
        return key;
    }

    private static OidcCallbackResult result(String accessToken) {
        return new OidcCallbackResult(accessToken, HOUR, "refresh");
    }

    private static Scope scope(String... values) {
        Scope scope = new Scope();
        for (String value : values) {
            scope.add(new Scope.Value(value));
        }
        return scope;
    }
}
