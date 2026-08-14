package com.dbschema.mongo.oidc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

  private static final Logger logger = Logger.getLogger(Server.class.getName());

  private static final int RESPONSE_TIMEOUT_SECONDS = 300;
  private static final int MAX_REPORTED_ERROR_LENGTH = 200;
  private static final int SERVER_THREAD_POOL_SIZE = 5;
  private static final String ACCEPTED_ENDPOINT = "/accepted";
  private static final String CALLBACK_ENDPOINT = "/callback";
  public static final String REDIRECT_ENDPOINT = "/redirect";
  private final int redirectPort;
  private final String redirectHost;
  private final String expectedState;

  private HttpServer server;
  private ExecutorService executor;private final BlockingQueue<OidcResponse> oidcResponseQueue;
  // written by the handler threads, read by the one waiting in getOidcResponse: what was refused for
  // carrying the wrong state, or none. A Server belongs to a single login, so this is about that login.
  private final AtomicInteger refusedCallbacks = new AtomicInteger();
  private final AtomicReference<String> lastRefusedError = new AtomicReference<>();

  /**
   * @param expectedState the state of the login this server is opened for; a callback carrying anything
   *                      else is refused and never reaches {@link #getOidcResponse(Duration)}
   */
  public Server(@NotNull String redirectHost, @NotNull String redirectPort, @NotNull String expectedState) {
    this.redirectHost = redirectHost;
    this.redirectPort = Integer.parseInt(redirectPort);
    this.expectedState = expectedState;
    oidcResponseQueue = new LinkedBlockingQueue<>();
  }

  /**
   * Starts the HTTP server on a random available port and sets up the necessary contexts and handlers.
   *
   * @throws IOException if an I/O error occurs while creating or starting the server
   */
  public void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress(this.redirectHost, this.redirectPort), 0);

    server.createContext(CALLBACK_ENDPOINT, new CallbackHandler());
    server.createContext(REDIRECT_ENDPOINT, new CallbackHandler());
    server.createContext(ACCEPTED_ENDPOINT, new AcceptedHandler());
    executor = Executors.newFixedThreadPool(SERVER_THREAD_POOL_SIZE);
    server.setExecutor(executor);

    server.start();
    logger.info("Server started on port " + this.redirectPort);
  }

  /**
   * Returns the port the server is listening on.
   * Only valid after {@link #start()} has been called.
   */
  public int getPort() {
    return this.server.getAddress().getPort();
  }

  public String getHost() {
    return this.server.getAddress().getHostName();
  }

  public String getRedirectHost() {
    return this.redirectHost;
  }

  /**
   * An absent Origin is allowed, and has to be: the redirect from the identity provider is a top-level
   * browser navigation, which sends no Origin at all. When the header is there it must name this very
   * server -- a prefix match would accept 'http://localhost.evil.com'.
   *
   * @param redirectHost the host the server is bound to, which an Origin may name as well as the loopback
   *                     literals: it is configurable, and hardcoding the literals refused a callback to any
   *                     other configured host, the login then hanging until the response timeout
   * @param port         the port the server actually listens on
   */
  public static boolean isAllowedOrigin(@Nullable String origin, @NotNull String redirectHost, int port) {
    if (origin == null) return true;
    URI uri;
    try {
      uri = new URI(origin);
    }
    catch (URISyntaxException e) {
      return false;
    }
    String scheme = uri.getScheme();
    String host = uri.getHost();
    if (scheme == null || host == null) return false;
    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) return false;
    if (portOf(uri, scheme) != port) return false;
    String normalized = normalizeHost(host);
    return "localhost".equals(normalized) || "127.0.0.1".equals(normalized) || "::1".equals(normalized)
        || normalized.equals(normalizeHost(redirectHost));
  }

  /**
   * An Origin carries no port when it is the default one for its scheme, so 'http://localhost' names this
   * server whenever it listens on port 80. Comparing {@link URI#getPort()} directly reads that as -1 and
   * refuses it.
   */
  private static int portOf(@NotNull URI uri, @NotNull String scheme) {
    int port = uri.getPort();
    if (port != -1) return port;
    return "https".equalsIgnoreCase(scheme) ? 443 : 80;
  }

  /** URI keeps the brackets of an IPv6 literal in getHost(); the configured host may carry none. */
  private static String normalizeHost(@NotNull String host) {
    String normalized = host.trim().toLowerCase(Locale.ROOT);
    return normalized.startsWith("[") && normalized.endsWith("]")
        ? normalized.substring(1, normalized.length() - 1)
        : normalized;
  }

  public OidcResponse getOidcResponse() throws InterruptedException, OidcTimeoutException {
    return getOidcResponse(Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS));
  }

  public OidcResponse getOidcResponse(Duration timeout)
      throws OidcTimeoutException, InterruptedException {
    if (timeout == null) {
      return getOidcResponse();
    }
    OidcResponse response = oidcResponseQueue.poll(timeout.getSeconds(), TimeUnit.SECONDS);
    if (response == null) {
      throw new OidcTimeoutException(timeoutMessage());
    }
    return response;
  }

  /**
   * Names what was refused while this login was waiting. Without it a provider error that arrived without
   * the state -- refused, and rightly so, since an unsolicited request looks exactly the same -- would be
   * lost entirely, and the connection would report nothing but a timeout.
   */
  private String timeoutMessage() {
    int refused = refusedCallbacks.get();
    if (refused == 0) return "Timeout waiting for OIDC response";
    String error = lastRefusedError.get();
    return "Timeout waiting for OIDC response. " + refused + " callback(s) reached the port without the "
        + "state of this login and were refused"
        + (error == null ? "" : ", the last of them reporting " + error)
        + ". An identity provider that does not echo the state parameter back is indistinguishable from an "
        + "unsolicited request here, and a callback of either kind cannot be allowed to end a login.";
  }

  public void stop() {
    if (server != null) {
      server.stop(0);
    }
    if (executor != null) {
      executor.shutdownNow();
    }
  }

  private class CallbackHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
      if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
        reply(exchange, HttpURLConnection.HTTP_BAD_METHOD);
        return;
      }

      String origin = exchange.getRequestHeaders().getFirst("Origin");
      if (!isAllowedOrigin(origin)) {
        logger.log(Level.WARNING, "Rejected callback from unexpected origin: {0}", origin);
        reply(exchange, HttpURLConnection.HTTP_FORBIDDEN);
        return;
      }

      Map<String, String> queryParams = parseQueryParams(exchange);

      // Nothing reaches the queue unless it carries the state of the login in progress. The flow takes
      // whatever comes out of the queue first, so a request with a foreign state would otherwise win that
      // race and abort a login the user is in the middle of -- which anything able to make the browser
      // issue a request to this port can do, the Origin header being absent on a top-level navigation and
      // therefore unusable as the gate. The state values are never logged: they are single-use secrets.
      String state = queryParams.get("state");
      if (!isExpectedState(state)) {
        refusedCallbacks.incrementAndGet();
        String error = describeError(queryParams);
        if (error == null) {
          logger.log(Level.WARNING, "Rejected a callback that does not carry the state of the login in " +
              "progress: an unsolicited request, or a provider that did not echo the state parameter back");
        }
        else {
          // reported here and again in the timeout, because it is all this login will ever learn about
          // the failure: the error cannot be acted on, an unsolicited request being able to claim one too
          lastRefusedError.set(error);
          logger.log(Level.WARNING, "Rejected a callback reporting {0}: it does not carry the state of the " +
              "login in progress. A provider that does not echo the state parameter back on its error " +
              "redirect cannot be told apart from an unsolicited request, so this login keeps waiting for " +
              "a callback that does, and will end in a timeout.", error);
        }
        reply(exchange, HttpURLConnection.HTTP_FORBIDDEN);
        return;
      }

      if (queryParams.containsKey("code")) {
        OidcResponse oidcResponse = OidcResponse.success(queryParams.get("code"), state);
        if (!putOidcResponse(exchange, oidcResponse)) return;

        exchange.getResponseHeaders().set("Location", ACCEPTED_ENDPOINT);
        reply(exchange, HttpURLConnection.HTTP_MOVED_TEMP);
      }
      else if (queryParams.containsKey("error")) {
        OidcResponse oidcResponse = OidcResponse.error(
            queryParams.get("error"),
            queryParams.getOrDefault("error_description", "Unknown error"),
            state);
        if (!putOidcResponse(exchange, oidcResponse)) return;
        reply(exchange, HttpURLConnection.HTTP_BAD_REQUEST);
      }
      else {
        // the state is left out: it passed the gate above, so it is the genuine single-use secret, and
        // this description is reported as the error of the login and ends up in a log
        String allParams = queryParams.entrySet().stream()
            .filter(entry -> !"state".equals(entry.getKey()))
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((a, b) -> a + ", " + b)
            .orElse("No parameters");
        OidcResponse oidcResponse = OidcResponse.error("Not found", "Not found. Parameters: " + allParams, state);
        if (!putOidcResponse(exchange, oidcResponse)) return;
        reply(exchange, HttpURLConnection.HTTP_NOT_FOUND);
      }
    }

    /**
     * The error a refused callback claimed, {@code null} when it claimed none. Sanitized: it comes from a
     * request nothing has authenticated and is written to a log record and to an exception message, so a
     * newline in it would forge a log line of its own.
     */
    @Nullable
    private String describeError(@NotNull Map<String, String> queryParams) {
      String error = queryParams.get("error");
      if (error == null) return null;
      String description = queryParams.get("error_description");
      return sanitize("error=" + error + (description == null ? "" : " (" + description + ")"));
    }

    private String sanitize(@NotNull String value) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < value.length() && sb.length() < MAX_REPORTED_ERROR_LENGTH; i++) {
        char c = value.charAt(i);
        sb.append(c >= ' ' && c < 127 ? c : ' ');
      }
      return value.length() > sb.length() ? sb.append("...").toString() : sb.toString();
    }

    /**
     * A missing state parameter counts as a mismatch. Compared in constant time, as the state is what
     * makes a callback belong to this login.
     */
    private boolean isExpectedState(@Nullable String state) {
      byte[] expected = expectedState.getBytes(StandardCharsets.UTF_8);
      byte[] actual = state == null ? new byte[0] : state.getBytes(StandardCharsets.UTF_8);
      return MessageDigest.isEqual(expected, actual);
    }

    private boolean isAllowedOrigin(@Nullable String origin) {
      return Server.isAllowedOrigin(origin, redirectHost, getPort());
    }

    private Map<String, String> parseQueryParams(HttpExchange exchange) {
      Map<String, String> queryParams = new HashMap<>();
      String rawQuery = exchange.getRequestURI().getRawQuery();
      if (rawQuery == null) return queryParams;

      String[] params = rawQuery.split("&");
      for (String param : params) {
        int equalsIndex = param.indexOf('=');
        if (equalsIndex > 0) {
          String key = param.substring(0, equalsIndex);
          String value = URLDecoder.decode(param.substring(equalsIndex + 1), StandardCharsets.UTF_8);
          queryParams.put(key, value);
        }
        else {
          queryParams.put(param, "");
        }
      }
      return queryParams;
    }

    private boolean putOidcResponse(HttpExchange exchange, OidcResponse oidcResponse)
        throws IOException {
      try {
        oidcResponseQueue.put(oidcResponse);
        return true;
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        reply(exchange, 500);
        return false;
      }
    }
  }

  private class AcceptedHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
      String html = loadResource("/oidc/accepted.html");
      replyWithBody(exchange, HttpURLConnection.HTTP_OK, html);
    }
  }

  private String loadResource(String path) throws IOException {
    try (InputStream is = getClass().getResourceAsStream(path)) {
      if (is == null) {
        throw new IOException("Resource not found: " + path);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private void reply(HttpExchange exchange, int statusCode) throws IOException {
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
    try (exchange) {
      exchange.sendResponseHeaders(statusCode, -1);
    }
    catch (Exception e) {
      logger.log(Level.SEVERE, "Error sending response", e);
      throw e;
    }
  }

  private void replyWithBody(HttpExchange exchange, int statusCode, String body) throws IOException {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
    try (exchange) {
      exchange.sendResponseHeaders(statusCode, bytes.length);
      exchange.getResponseBody().write(bytes);
    }
    catch (Exception e) {
      logger.log(Level.SEVERE, "Error sending response", e);
      throw e;
    }
  }
}
