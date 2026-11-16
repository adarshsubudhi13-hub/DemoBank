package com.demobank.server;

import com.demobank.auth.SessionStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Handles session revocation requests at POST /api/logout.
 */
public final class LogoutHandler implements HttpHandler {
    private final SessionStore sessionStore;

    public LogoutHandler(SessionStore sessionStore) {
        this.sessionStore = sessionStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendJson(exchange, 405, "{\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Only POST allowed\"}");
            return;
        }

        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null) {
            for (String pair : cookie.split(";")) {
                String[] parts = pair.trim().split("=", 2);
                if ("SID".equalsIgnoreCase(parts[0]) && parts.length > 1) {
                    sessionStore.invalidate(parts[1].trim());
                }
            }
        }

        exchange.getResponseHeaders().add("Set-Cookie", "SID=; Max-Age=0; HttpOnly; SameSite=Strict; Path=/");
        HttpUtil.sendJson(exchange, 200, "{\"success\":true,\"message\":\"Logged out\"}");
    }
}
