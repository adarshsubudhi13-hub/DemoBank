package com.demobank.server;

import com.demobank.account.Account;
import com.demobank.account.AccountRepository;
import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.security.SecurityState;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Handles account queries and public demo telemetry stats.
 */
public final class AccountHandler implements HttpHandler {
    private final AuthService authService;
    private final UserRepository userRepo;
    private final AccountRepository accountRepo;
    private final SessionStore sessionStore;
    private final SecurityState securityState;
    private final Gson gson = new Gson();

    public AccountHandler(AuthService authService, UserRepository userRepo,
                          AccountRepository accountRepo, SessionStore sessionStore,
                          SecurityState securityState) {
        this.authService = authService;
        this.userRepo = userRepo;
        this.accountRepo = accountRepo;
        this.sessionStore = sessionStore;
        this.securityState = securityState;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/api/stats".equalsIgnoreCase(path)) {
            handleStats(exchange);
        } else if ("/api/account".equalsIgnoreCase(path)) {
            handleAccount(exchange);
        } else {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"NOT_FOUND\"}");
        }
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        long attempts = authService.getAttempts();
        long failures = authService.getFailures();
        int blockedIps = securityState.getBlockedIps().size();
        int lockedUsers = securityState.getLockedUsers().size();

        String json = String.format("{\"attempts\":%d,\"failures\":%d,\"blockedIps\":%d,\"lockedUsers\":%d}",
            attempts, failures, blockedIps, lockedUsers);
        HttpUtil.sendJson(exchange, 200, json);
    }

    private void handleAccount(HttpExchange exchange) throws IOException {
        String token = extractSid(exchange);
        var userOpt = sessionStore.getUsername(token);
        if (userOpt.isEmpty()) {
            HttpUtil.sendJson(exchange, 401, "{\"error\":\"UNAUTHORIZED\",\"message\":\"Session expired or missing\"}");
            return;
        }

        var userRec = userRepo.findByUsername(userOpt.get());
        if (userRec.isEmpty()) {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"NOT_FOUND\",\"message\":\"User not found\"}");
            return;
        }

        var accOpt = accountRepo.findById(userRec.get().accountId());
        if (accOpt.isEmpty()) {
            HttpUtil.sendJson(exchange, 404, "{\"error\":\"NOT_FOUND\",\"message\":\"Account not found\"}");
            return;
        }

        HttpUtil.sendJson(exchange, 200, gson.toJson(accOpt.get()));
    }

    private String extractSid(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie == null) return null;
        for (String pair : cookie.split(";")) {
            String[] parts = pair.trim().split("=", 2);
            if ("SID".equalsIgnoreCase(parts[0]) && parts.length > 1) {
                return parts[1].trim();
            }
        }
        return null;
    }
}
