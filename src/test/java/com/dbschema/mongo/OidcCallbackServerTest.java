package com.dbschema.mongo;

import com.dbschema.mongo.oidc.OidcResponse;
import com.dbschema.mongo.oidc.OidcTimeoutException;
import com.dbschema.mongo.oidc.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.dbschema.mongo.oidc.Server.REDIRECT_ENDPOINT;
import static com.dbschema.mongo.oidc.Server.isAllowedOrigin;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class OidcCallbackServerTest {
    private static final String STATE = "s0me-256-bit-state-value";
    private static final Duration SHORT = Duration.ofSeconds(1);

    private Server server;

    @Before
    public void startServer() throws Exception {
        server = new Server("localhost", "0", STATE);
        server.start();
    }

    @After
    public void stopServer() {
        if (server != null) server.stop();
    }

    /**
     * The bug this guards against: every request carrying a 'code' used to be queued, and the flow takes
     * whatever comes out of the queue first. So a forged 'GET /redirect?code=...&state=WRONG' arriving while
     * a login was in flight was dequeued ahead of the genuine redirect and the user's authentication died
     * with 'invalid state' -- a denial of service anything able to make the browser hit the callback port
     * could trigger. The forged request is now refused and the genuine one is still the one delivered.
     */
    @Test
    public void aCallbackWithAForeignStateNeitherIsQueuedNorDisplacesTheRealOne() throws Exception {
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, get(callbackUrl("code=stolen&state=WRONG"), null));
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, get(callbackUrl("code=genuine&state=" + STATE), null));

        OidcResponse response = server.getOidcResponse(SHORT);
        assertEquals("genuine", response.getCode());
        assertEquals(STATE, response.getState());
        assertNull(response.getError());
    }

    /** A missing state parameter is a mismatch too, and a request without one must not be queued. */
    @Test
    public void aCallbackWithoutAStateIsRefused() throws Exception {
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, get(callbackUrl("code=stolen"), null));
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, get(callbackUrl(null), null));

        assertThrows(OidcTimeoutException.class, () -> server.getOidcResponse(SHORT));
    }

    /**
     * The state gate refuses an error redirect that does not echo the state back -- it cannot be told from
     * an unsolicited one, so it must not end a login. What it reported used to be dropped with it, and the
     * connection failed up to 300 s later with a bare timeout, saying nothing of the access_denied that
     * had arrived within seconds. It is named in the timeout now, and logged when it is refused.
     */
    @Test
    public void aStatelessProviderErrorIsRefusedButNamedInTheTimeout() throws Exception {
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN,
            get(callbackUrl("error=access_denied&error_description=User+declined"), null));

        String message = timeoutMessage();
        assertTrue(message, message.contains("access_denied"));
        assertTrue(message, message.contains("User declined"));
        assertTrue(message, message.contains("refused"));
    }

    @Test
    public void aRefusedCallbackWithoutAnErrorIsOnlyCounted() throws Exception {
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN, get(callbackUrl("code=stolen&state=WRONG"), null));

        String message = timeoutMessage();
        assertTrue(message, message.contains("1 callback(s)"));
        assertFalse(message, message.contains("reporting"));
    }

    /** The ordinary case -- the user never finished the login -- must not grow the new wording. */
    @Test
    public void aLoginNothingReachedKeepsThePlainTimeout() {
        assertEquals("Timeout waiting for OIDC response", timeoutMessage());
    }

    /** The description comes from an unauthenticated request and lands in a log record. */
    @Test
    public void theDescriptionOfARefusedErrorCannotForgeALogLine() throws Exception {
        String description = "line%0D%0ASEVERE:+forged" + "+padding".repeat(50);
        assertEquals(HttpURLConnection.HTTP_FORBIDDEN,
            get(callbackUrl("error=access_denied&error_description=" + description), null));

        String message = timeoutMessage();
        assertFalse(message, message.contains("\n"));
        assertFalse(message, message.contains("\r"));
        assertTrue(message, message.contains("line  SEVERE: forged"));
        // capped, so the padding cannot push the rest of the message out of sight
        assertTrue(message, message.contains("..."));
    }

    /** The genuine redirect is a top-level browser navigation: it carries no Origin header at all. */
    @Test
    public void theCallbackEndpointAcceptsBothPathsWithNoOrigin() throws Exception {
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP, get(callbackUrl("code=first&state=" + STATE), null));
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP,
            get(url("/callback", "code=second&state=" + STATE), null));

        assertEquals("first", server.getOidcResponse(SHORT).getCode());
        assertEquals("second", server.getOidcResponse(SHORT).getCode());
    }

    /**
     * The Origin check used to be a prefix match, so 'http://localhost.evil.com' passed it. Only this very
     * server -- loopback host and its own port -- is accepted now.
     */
    @Test
    public void anOriginOtherThanThisServerIsRefused() throws Exception {
        String query = "code=stolen&state=" + STATE;
        for (String origin : new String[]{
            "http://localhost.evil.com",
            "https://localhost.evil.com:" + server.getPort(),
            "http://localhost:" + (server.getPort() + 1),
            "http://evil.com:" + server.getPort(),
            "http://localhost",
            "not a url",
            ""}) {
            assertEquals(origin, HttpURLConnection.HTTP_FORBIDDEN, get(callbackUrl(query), origin));
        }

        assertThrows(OidcTimeoutException.class, () -> server.getOidcResponse(SHORT));
    }

    @Test
    public void theOriginOfThisServerIsAccepted() throws Exception {
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP,
            get(callbackUrl("code=genuine&state=" + STATE), "http://localhost:" + server.getPort()));
        assertEquals(HttpURLConnection.HTTP_MOVED_TEMP,
            get(callbackUrl("code=genuine2&state=" + STATE), "http://127.0.0.1:" + server.getPort()));

        assertEquals("genuine", server.getOidcResponse(SHORT).getCode());
        assertEquals("genuine2", server.getOidcResponse(SHORT).getCode());
    }

    /**
     * The callback host is configurable, and the Origin check used to compare against the loopback literals
     * only: a callback to any other configured host was answered 403 and the login then hung until the
     * response timeout. Checked directly -- binding a foreign host is not portable enough for a test.
     */
    @Test
    public void anOriginNamingTheConfiguredCallbackHostIsAccepted() {
        assertTrue(isAllowedOrigin("http://db.mycorp.net:27097", "db.mycorp.net", 27097));
        // the loopback literals stay allowed whatever the configured host is
        assertTrue(isAllowedOrigin("http://localhost:27097", "db.mycorp.net", 27097));
        assertTrue(isAllowedOrigin("http://127.0.0.1:27097", "db.mycorp.net", 27097));
        // matched case insensitively, and an IPv6 literal whether or not the brackets are configured
        assertTrue(isAllowedOrigin("http://DB.MyCorp.NET:27097", "db.mycorp.net", 27097));
        assertTrue(isAllowedOrigin("http://[fe80::1]:27097", "fe80::1", 27097));
        assertTrue(isAllowedOrigin("http://[fe80::1]:27097", "[fe80::1]", 27097));
    }

    /** Still an exact host match and still this server's port: neither is relaxed by the host being one more. */
    @Test
    public void anOriginNamingAnotherHostOrPortIsStillRefused() {
        assertFalse(isAllowedOrigin("http://db.mycorp.net.evil.com:27097", "db.mycorp.net", 27097));
        assertFalse(isAllowedOrigin("http://evil.db.mycorp.net:27097", "db.mycorp.net", 27097));
        assertFalse(isAllowedOrigin("http://db.mycorp.net:27098", "db.mycorp.net", 27097));
        assertFalse(isAllowedOrigin("ftp://db.mycorp.net:27097", "db.mycorp.net", 27097));
        assertFalse(isAllowedOrigin("http://localhost.evil.com:27097", "localhost", 27097));
    }

    /**
     * An Origin omits the port when it is the default one for its scheme, and 'uri.getPort() != port' read
     * that as -1 and refused it -- so 'http://localhost' was rejected by a server listening on port 80.
     */
    @Test
    public void anOriginWithoutAPortMatchesTheDefaultPortOfItsScheme() {
        assertTrue(isAllowedOrigin("http://localhost", "localhost", 80));
        assertTrue(isAllowedOrigin("https://localhost", "localhost", 443));
        assertFalse(isAllowedOrigin("http://localhost", "localhost", 443));
        assertFalse(isAllowedOrigin("https://localhost", "localhost", 80));
        assertFalse(isAllowedOrigin("http://localhost", "localhost", 27097));
    }

    /** A provider error is delivered, with its state, so the flow can report the error instead of a timeout. */
    @Test
    public void aProviderErrorCarryingTheStateIsDelivered() throws Exception {
        assertEquals(HttpURLConnection.HTTP_BAD_REQUEST,
            get(callbackUrl("error=access_denied&error_description=nope&state=" + STATE), null));

        OidcResponse response = server.getOidcResponse(SHORT);
        assertEquals("access_denied", response.getError());
        assertEquals("nope", response.getErrorDescription());
        assertEquals(STATE, response.getState());
        assertNull(response.getCode());
    }

    /** The message of the timeout the flow would fail with, which is where a refusal has to show up. */
    private String timeoutMessage() {
        return assertThrows(OidcTimeoutException.class, () -> server.getOidcResponse(SHORT)).getMessage();
    }

    private String callbackUrl(String query) {
        return url(REDIRECT_ENDPOINT, query);
    }

    private String url(String path, String query) {
        return "http://localhost:" + server.getPort() + path + (query == null ? "" : "?" + query);
    }

    /**
     * HttpClient rather than HttpURLConnection, which drops an 'Origin' request header silently: it is on
     * its list of restricted headers, so the Origin cases would all be testing a request without one.
     */
    private static int get(String url, String origin) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).GET();
        if (origin != null) request.header("Origin", origin);
        // the default redirect policy is NEVER, so the 302 to /accepted is reported as itself
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.discarding())
            .statusCode();
    }
}
