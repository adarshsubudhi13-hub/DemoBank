package com.demobank.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Liveness and health check endpoint at GET /health.
 */
public final class HealthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtil.sendJson(exchange, 200, "{\"status\":\"ok\"}");
    }
}
