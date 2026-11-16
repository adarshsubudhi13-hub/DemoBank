package com.demobank.auth;

import com.demobank.config.Config;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory session store managing 32-hex SecureRandom tokens with 15-minute expiry.
 */
public final class SessionStore {
    public record Session(String token, String username, Instant createdAt, Instant expiresAt) {}

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public String createSession(String username) {
        byte[] bytes = new byte[16]; // 32 hex chars
        random.nextBytes(bytes);
        String token = HexFormat.of().formatHex(bytes);
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(Config.getSessionMinutes() * 60L);
        sessions.put(token, new Session(token, username, now, expiry));
        return token;
    }

    public Optional<String> getUsername(String token) {
        if (token == null) return Optional.empty();
        Session session = sessions.get(token);
        if (session == null) return Optional.empty();
        if (Instant.now().isAfter(session.expiresAt())) {
            sessions.remove(token, session);
            return Optional.empty();
        }
        return Optional.of(session.username());
    }

    public boolean invalidate(String token) {
        if (token == null) return false;
        return sessions.remove(token) != null;
    }

    public void clear() {
        sessions.clear();
    }
}
