package com.dbschema.mongo;

import com.dbschema.mongo.oidc.OidcAuthFlow;
import com.nimbusds.oauth2.sdk.TokenRequest;
import org.junit.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class OidcRefreshRequestTest {
    private static final URI TOKEN_ENDPOINT = URI.create("https://login.microsoftonline.com/tenant/oauth2/v2.0/token");
    private static final String CLIENT_ID = "11111111-2222-3333-4444-555555555555";
    private static final String REFRESH_TOKEN = "refresh-token-value";

    /**
     * The bug this guards against: the refresh_token grant used to echo the scopes requested by the server.
     * Per RFC 6749 section 6 the refresh scope must not contain anything that was never granted, so a
     * provider that granted only a subset answered with 'invalid_scope'; the cached entry was then
     * invalidated and the user was pushed through a full browser login on every access token expiry.
     * Sending no scope keeps the originally granted one, which is the audience guarantee we want.
     */
    @Test
    public void refreshRequestSendsNoScope() throws Exception {
        assertFalse(formParameters().containsKey("scope"));
    }

    @Test
    public void refreshRequestCarriesGrantTypeAndToken() throws Exception {
        Map<String, List<String>> parameters = formParameters();

        assertEquals(List.of("refresh_token"), parameters.get("grant_type"));
        assertEquals(List.of(REFRESH_TOKEN), parameters.get("refresh_token"));
        assertEquals(List.of(CLIENT_ID), parameters.get("client_id"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> formParameters() throws Exception {
        TokenRequest request = OidcAuthFlow.buildRefreshTokenRequest(TOKEN_ENDPOINT, CLIENT_ID, REFRESH_TOKEN);
        return request.toHTTPRequest().getBodyAsFormParameters();
    }
}
