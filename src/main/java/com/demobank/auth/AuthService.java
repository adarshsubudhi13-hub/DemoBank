package com.demobank.auth;

import com.demobank.logging.AuthLogEntry;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Authentication service coordinating credential validation, security state, and audit logging.
 */
public final class AuthService {
    public record LoginResult(int status, String token, String reason, String message) {}

    private final UserRepository userRepo;
    private final SecurityState securityState;
    private final SessionStore sessionStore;
    private final AuthLogger authLogger;
    private final AtomicLong attempts = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();

    public AuthService(UserRepository userRepo, SecurityState securityState,
                       SessionStore sessionStore, AuthLogger authLogger) {
        this.userRepo = userRepo;
        this.securityState = securityState;
        this.sessionStore = sessionStore;
        this.authLogger = authLogger;
    }

    public LoginResult login(String username, String password, String clientIp) {
        if (username == null || username.isBlank() || password == null || password.isBlank() || username.length() > 64) {
            return new LoginResult(400, null, "MALFORMED", "Invalid username or password format");
        }

        String trimmedUser = username.trim();
        attempts.incrementAndGet();

        // 1. IP Blocked check
        if (securityState.isIpBlocked(clientIp)) {
            failures.incrementAndGet();
            logAttempt(clientIp, trimmedUser, false, "IP_BLOCKED", 403);
            return new LoginResult(403, null, "IP_BLOCKED", "Your IP address is blocked.");
        }

        // 2. User exists check
        var userOpt = userRepo.findByUsername(trimmedUser);
        if (userOpt.isEmpty()) {
            failures.incrementAndGet();
            logAttempt(clientIp, trimmedUser, false, "UNKNOWN_USER", 401);
            return new LoginResult(401, null, "UNKNOWN_USER", "Invalid username or password.");
        }

        // 3. Account locked check
        if (securityState.isUserLocked(trimmedUser)) {
            failures.incrementAndGet();
            logAttempt(clientIp, trimmedUser, false, "ACCOUNT_LOCKED", 423);
            return new LoginResult(423, null, "ACCOUNT_LOCKED", "This account is temporarily locked.");
        }

        // 4. Password check
        var user = userOpt.get();
        String computedHash = PasswordHasher.hash(password, user.salt());
        if (!computedHash.equals(user.sha256hash())) {
            failures.incrementAndGet();
            logAttempt(clientIp, trimmedUser, false, "BAD_PASSWORD", 401);
            return new LoginResult(401, null, "BAD_PASSWORD", "Invalid username or password.");
        }

        // 5. Success
        String token = sessionStore.createSession(user.username());
        logAttempt(clientIp, trimmedUser, true, "NONE", 200);
        return new LoginResult(200, token, "NONE", "Authentication successful");
    }

    private void logAttempt(String ip, String username, boolean success, String reason, int status) {
        AuthLogEntry entry = new AuthLogEntry(Instant.now().toString(), ip, username, success, reason, status);
        authLogger.log(entry);
    }

    public long getAttempts() { return attempts.get(); }
    public long getFailures() { return failures.get(); }

    public void resetCounters() {
        attempts.set(0);
        failures.set(0);
    }
}
