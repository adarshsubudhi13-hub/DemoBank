package com.demobank.server;

import com.demobank.auth.AuthService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Handles customer authentication requests at POST /api/login with session cookie issuance.
 */
public final class LoginHandler implements HttpHandler {
    private final AuthService authService;
    private final Gson gson = new Gson();

    public LoginHandler(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            HttpUtil.sendJson(exchange, 405, "{\"error\":\"METHOD_NOT_ALLOWED\",\"message\":\"Only POST allowed\"}");
            return;
        }

        String body;
        try {
            body = HttpUtil.readBody(exchange);
        } catch (HttpUtil.BodyTooLargeException e) {
            HttpUtil.sendJson(exchange, 413, "{\"error\":\"PAYLOAD_TOO_LARGE\",\"message\":\"Body exceeds 4 KB\"}");
            return;
        }

        String username = null;
        String password = null;
        try {
            JsonObject json = gson.fromJson(body, JsonObject.class);
            if (json != null) {
                if (json.has("username") && !json.get("username").isJsonNull()) {
                    username = json.get("username").getAsString();
                }
                if (json.has("password") && !json.get("password").isJsonNull()) {
                    password = json.get("password").getAsString();
                }
            }
        } catch (JsonSyntaxException e) {
            HttpUtil.sendJson(exchange, 400, "{\"error\":\"BAD_REQUEST\",\"message\":\"Malformed JSON payload\"}");
            return;
        }

        String clientIp = HttpUtil.resolveClientIp(exchange);
        AuthService.LoginResult res = authService.login(username, password, clientIp);

        if (res.status() == 200) {
            exchange.getResponseHeaders().add("Set-Cookie",
                "SID=" + res.token() + "; HttpOnly; SameSite=Strict; Path=/");
            HttpUtil.sendJson(exchange, 200,
                "{\"success\":true,\"username\":\"" + username + "\",\"message\":\"" + res.message() + "\"}");
        } else {
            HttpUtil.sendJson(exchange, res.status(),
                "{\"success\":false,\"error\":\"" + res.reason() + "\",\"message\":\"" + res.message() + "\"}");
        }
    }
}
