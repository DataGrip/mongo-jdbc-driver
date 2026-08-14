package com.dbschema.mongo;

import com.dbschema.mongo.oidc.OidcAuthFlow;
import com.mongodb.MongoCredential.IdpInfo;
import com.nimbusds.oauth2.sdk.Scope;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class OidcBuildScopesTest {
    private static final String CLIENT_ID = "11111111-2222-3333-4444-555555555555";
    private static final String RESOURCE_SCOPE = "api://" + CLIENT_ID + "/psmdb-access";

    /**
     * The bug this guards against: a resource scope requested by the server used to be dropped because it is
     * not listed in the identity provider's advertised 'scopes_supported', which made the provider issue a
     * token for the wrong audience and the server reject it with 'AuthenticationFailed'.
     */
    @Test
    public void serverRequestedResourceScopeIsRequested() {
        assertEquals(scope("openid", "offline_access", RESOURCE_SCOPE),
            flow().buildScopes(idpInfo(RESOURCE_SCOPE)));
    }

    @Test
    public void everyServerRequestedScopeIsRequested() {
        assertEquals(scope("openid", "offline_access", RESOURCE_SCOPE, "email"),
            flow().buildScopes(idpInfo(RESOURCE_SCOPE, "email")));
    }

    /**
     * Scope is a set, so its equals ignores order; the order still matters because it goes into the token
     * cache key, hence the assertion on the string form.
     */
    @Test
    public void requestedScopesAreOrderedDeterministically() {
        assertEquals("openid offline_access " + RESOURCE_SCOPE + " email",
            flow().buildScopes(idpInfo(RESOURCE_SCOPE, "email")).toString());
    }

    @Test
    public void scopesAreTrimmedAndDeduplicated() {
        assertEquals(scope("openid", "offline_access", RESOURCE_SCOPE),
            flow().buildScopes(idpInfo("openid", " " + RESOURCE_SCOPE + " ", "offline_access", RESOURCE_SCOPE, "")));
    }

    /**
     * No scope is guessed when the server requests none: which extra scope an identity provider needs is
     * provider-specific, and requesting a wrong one is rejected by the provider.
     */
    @Test
    public void withoutAnyRequestedScopeOnlyTheOidcScopesAreRequested() {
        assertEquals(scope("openid", "offline_access"), flow().buildScopes(idpInfo()));
        assertEquals(scope("openid", "offline_access"), flow().buildScopes(idpInfoWithScopes(Collections.emptyList())));
    }

    private static OidcAuthFlow flow() {
        return new OidcAuthFlow("localhost", "27097");
    }

    private static Scope scope(String... values) {
        Scope scope = new Scope();
        for (String value : values) {
            scope.add(new Scope.Value(value));
        }
        return scope;
    }

    private static IdpInfo idpInfo(String... requestScopes) {
        return idpInfoWithScopes(asList(requestScopes));
    }

    private static IdpInfo idpInfoWithScopes(List<String> requestScopes) {
        return new IdpInfo() {
            @Override
            public String getIssuer() {
                return "https://login.microsoftonline.com/tenant/v2.0";
            }

            @Override
            public String getClientId() {
                return CLIENT_ID;
            }

            @Override
            public List<String> getRequestScopes() {
                return requestScopes;
            }
        };
    }
}
