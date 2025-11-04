package com.dbschema.mongo.oidc;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    private static final Logger logger = Logger.getLogger(Server.class.getName());

    public static final int DEFAULT_REDIRECT_PORT = 27098;
    private static final String ACCEPTED_ENDPOINT = "/accepted";

    private HttpServer server;
    private final BlockingQueue<OidcResponse> oidcResponseQueue;

    public Server() {
        oidcResponseQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Starts the HTTP server and sets up the necessary contexts and handlers.
     *
     * @throws IOException if an I/O error occurs while creating or starting the server
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(DEFAULT_REDIRECT_PORT), 0);

        server.createContext("/callback", new CallbackHandler());
        server.createContext("/redirect", new CallbackHandler());
        server.createContext(ACCEPTED_ENDPOINT, new AcceptedHandler());
        server.setExecutor(Executors.newFixedThreadPool(5));

        // Start the server
        server.start();
        logger.info("Server started on port " + DEFAULT_REDIRECT_PORT);
    }

    public OidcResponse getOidcResponse() throws InterruptedException, OidcTimeoutException {
        return getOidcResponse(Duration.ofSeconds(300));
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
    }

    private class CallbackHandler implements HttpHandler {

        private Map<String, String> parseQueryParams(HttpExchange exchange) {
            Map<String, String> queryParams = new HashMap<>();
            String rawQuery = exchange.getRequestURI().getRawQuery();

            if (rawQuery != null) {
                String[] params = rawQuery.split("&");
                for (String param : params) {
                    int equalsIndex = param.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = param.substring(0, equalsIndex);
                        String encodedValue = param.substring(equalsIndex + 1);
                        String value = URLDecoder.decode(encodedValue, StandardCharsets.UTF_8);
                        queryParams.put(key, value);
                    } else {
                        queryParams.put(param, "");
                    }
                }
            }
            return queryParams;
        }

        private boolean putOidcResponse(HttpExchange exchange, OidcResponse oidcResponse)
                throws IOException {
            try {
                oidcResponseQueue.put(oidcResponse);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                reply(exchange, 500);
                return false;
            }
        }


        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> queryParams = parseQueryParams(exchange);
            OidcResponse oidcResponse = new OidcResponse();

            if (queryParams.containsKey("code")) {
                oidcResponse.setCode(queryParams.get("code"));
                oidcResponse.setState(queryParams.getOrDefault("state", ""));
                if (!putOidcResponse(exchange, oidcResponse)) {
                    return;
                }

                exchange.getResponseHeaders().set("Location", ACCEPTED_ENDPOINT);
                reply(exchange, HttpURLConnection.HTTP_MOVED_TEMP);
            } else if (queryParams.containsKey("error")) {
                oidcResponse.setError(queryParams.get("error"));
                oidcResponse.setErrorDescription(
                        queryParams.getOrDefault("error_description", "Unknown error"));
                if (!putOidcResponse(exchange, oidcResponse)) {
                    return;
                }
                reply(exchange, HttpURLConnection.HTTP_BAD_REQUEST);

            } else {
                oidcResponse.setError("Not found");
                String allParams =
                        queryParams
                                .entrySet()
                                .stream()
                                .map(entry -> entry.getKey() + "=" + entry.getValue())
                                .reduce((param1, param2) -> param1 + ", " + param2)
                                .orElse("No parameters");
                oidcResponse.setErrorDescription("Not found. Parameters: " + allParams);
                if (!putOidcResponse(exchange, oidcResponse)) {
                    return;
                }
                reply(exchange, HttpURLConnection.HTTP_NOT_FOUND);
            }
        }
    }

    private class AcceptedHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            reply(exchange, HttpURLConnection.HTTP_OK);
        }
    }

    private void reply(HttpExchange exchange, int statusCode)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        try (exchange) {
            exchange.sendResponseHeaders(statusCode, -1);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error sending response", e);
            throw e;
        }
    }
}
