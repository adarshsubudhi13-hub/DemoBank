package com.demobank.server;

import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.config.Config;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Handles administrative defense API calls for IP blocking, account lockout, and telemetry reset.
 */
public final class AdminHandler implements HttpHandler {
    private final SecurityState securityState;
    private final UserRepository userRepo;
    private final AuthService authService;
    private final SessionStore sessionStore;
    private final AuthLogger authLogger;
    private final Gson gson = new Gson();

    public AdminHandler(SecurityState securityState, UserRepository userRepo,
                        AuthService authService, SessionStore sessionStore, AuthLogger authLogger) {
        this.securityState = securityState;
        this.userRepo = userRepo;
        this.authService = authService;
        this.sessionStore = sessionStore;
        this.authLogger = authLogger;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("X-Admin-Token");
        if (token == null || !token.equals(Config.getAdminToken())) {
            HttpUtil.sendJson(exchange, 401, "{\"error\":\"UNAUTHORIZED\",\"message\":\"Invalid admin token\"}");
            return;
        }

        if (!HttpUtil.isLocalhost(exchange)) {
            HttpUtil.sendJson(exchange, 403, "{\"error\":\"FORBIDDEN\",\"message\":\"Localhost caller only\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        switch (path) {
            case "/admin/block" -> handleBlock(exchange, true);
            case "/admin/unblock" -> handleBlock(exchange, false);
            case "/admin/lock-user" -> handleLockUser(exchange);
            case "/admin/unlock-user" -> handleUnlockUser(exchange);
            case "/admin/status" -> handleStatus(exchange);
            case "/admin/reset" -> handleReset(exchange);
            default -> HttpUtil.sendJson(exchange, 404, "{\"error\":\"NOT_FOUND\"}");
        }
    }

    private void handleBlock(HttpExchange exchange, boolean block) throws IOException {
        JsonObject json = parseJson(exchange);
        if (json == null || !json.has("ip") || json.get("ip").getAsString().isBlank()) {
            HttpUtil.sendJson(exchange, 400, "{\"error\":\"BAD_REQUEST\",\"message\":\"IP is required\"}");
            return;
        }
        String ip = json.get("ip").getAsString().trim();
        boolean changed = block ? securityState.blockIp(ip) : securityState.unblockIp(ip);
        HttpUtil.sendJson(exchange, 200, "{\"changed\":" + changed + ",\"ip\":\"" + ip + "\"}");
    }

    private void handleLockUser(HttpExchange exchange) throws IOException {
        JsonObject json = parseJson(exchange);
        if (json == null || !json.has("username") || !json.has("seconds")) {
            HttpUtil.sendJson(exchange, 400, "{\"error\":\"BAD_REQUEST\",\"message\":\"Missing username or seconds\"}");
            return;
        }
        String user = json.get("username").getAsString().trim();
        int seconds = json.get("seconds").getAsInt();
        if (!userRepo.userExists(user) || seconds < 1 || seconds > 3600) {
            HttpUtil.sendJson(exchange, 400, "{\"error\":\"BAD_REQUEST\",\"message\":\"Invalid user or seconds range\"}");
            return;
        }
        boolean changed = securityState.lockUser(user, seconds);
        Instant until = securityState.getLockExpiry(user);
        HttpUtil.sendJson(exchange, 200, "{\"changed\":" + changed + ",\"until\":\"" + (until != null ? until.toString() : "") + "\"}");
    }

    private void handleUnlockUser(HttpExchange exchange) throws IOException {
        JsonObject json = parseJson(exchange);
        if (json == null || !json.has("username")) {
            HttpUtil.sendJson(exchange, 400, "{\"error\":\"BAD_REQUEST\",\"message\":\"Username is required\"}");
            return;
        }
        String user = json.get("username").getAsString().trim();
        boolean changed = securityState.unlockUser(user);
        HttpUtil.sendJson(exchange, 200, "{\"changed\":" + changed + ",\"username\":\"" + user + "\"}");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        JsonObject res = new JsonObject();
        res.addProperty("attempts", authService.getAttempts());
        res.addProperty("failures", authService.getFailures());
        res.add("blockedIps", gson.toJsonTree(securityState.getBlockedIps()));
        res.add("lockedUsers", gson.toJsonTree(securityState.getLockedUsers()));
        res.addProperty("logHealthy", authLogger.isHealthy());
        HttpUtil.sendJson(exchange, 200, gson.toJson(res));
    }

    private void handleReset(HttpExchange exchange) throws IOException {
        securityState.reset();
        sessionStore.clear();
        authService.resetCounters();
        authLogger.rotate();
        HttpUtil.sendJson(exchange, 200, "{\"reset\":true}");
    }

    private JsonObject parseJson(HttpExchange exchange) {
        try {
            String body = HttpUtil.readBody(exchange);
            return gson.fromJson(body, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }
}
