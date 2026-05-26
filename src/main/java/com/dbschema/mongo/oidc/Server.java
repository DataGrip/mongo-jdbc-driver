package com.dbschema.mongo.oidc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

  private static final Logger logger = Logger.getLogger(Server.class.getName());

  private static final int RESPONSE_TIMEOUT_SECONDS = 300;
  private static final int SERVER_THREAD_POOL_SIZE = 5;
  private static final String ACCEPTED_ENDPOINT = "/accepted";
  private static final String CALLBACK_ENDPOINT = "/callback";
  private static final String REDIRECT_ENDPOINT = "/redirect";

  private HttpServer server;
  private ExecutorService executor;
  private int port;
  private final BlockingQueue<OidcResponse> oidcResponseQueue;

  public Server() {
    oidcResponseQueue = new LinkedBlockingQueue<>();
  }

  /**
   * Starts the HTTP server on a random available port and sets up the necessary contexts and handlers.
   *
   * @throws IOException if an I/O error occurs while creating or starting the server
   */
  public void start() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

    server.createContext(CALLBACK_ENDPOINT, new CallbackHandler());
    server.createContext(REDIRECT_ENDPOINT, new CallbackHandler());
    server.createContext(ACCEPTED_ENDPOINT, new AcceptedHandler());
    executor = Executors.newFixedThreadPool(SERVER_THREAD_POOL_SIZE);
    server.setExecutor(executor);

    server.start();
    port = server.getAddress().getPort();
    logger.info("Server started on port " + port);
  }

  /**
   * Returns the port the server is listening on.
   * Only valid after {@link #start()} has been called.
   */
  public int getPort() {
    return port;
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
      throw new OidcTimeoutException("Timeout waiting for OIDC response");
    }
    return response;
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
      if (origin != null && !origin.startsWith("http://localhost") && !origin.startsWith("http://127.0.0.1")) {
        logger.log(Level.WARNING, "Rejected callback from unexpected origin: " + origin);
        reply(exchange, HttpURLConnection.HTTP_FORBIDDEN);
        return;
      }

      Map<String, String> queryParams = parseQueryParams(exchange);

      if (queryParams.containsKey("code")) {
        OidcResponse oidcResponse = OidcResponse.success(
            queryParams.get("code"),
            queryParams.getOrDefault("state", ""));
        if (!putOidcResponse(exchange, oidcResponse)) return;

        exchange.getResponseHeaders().set("Location", ACCEPTED_ENDPOINT);
        reply(exchange, HttpURLConnection.HTTP_MOVED_TEMP);
      }
      else if (queryParams.containsKey("error")) {
        OidcResponse oidcResponse = OidcResponse.error(
            queryParams.get("error"),
            queryParams.getOrDefault("error_description", "Unknown error"));
        if (!putOidcResponse(exchange, oidcResponse)) return;
        reply(exchange, HttpURLConnection.HTTP_BAD_REQUEST);
      }
      else {
        String allParams = queryParams.entrySet().stream()
            .map(entry -> entry.getKey() + "=" + entry.getValue())
            .reduce((a, b) -> a + ", " + b)
            .orElse("No parameters");
        OidcResponse oidcResponse = OidcResponse.error("Not found", "Not found. Parameters: " + allParams);
        if (!putOidcResponse(exchange, oidcResponse)) return;
        reply(exchange, HttpURLConnection.HTTP_NOT_FOUND);
      }
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
