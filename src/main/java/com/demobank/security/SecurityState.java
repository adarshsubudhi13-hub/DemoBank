package com.demobank.security;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe security state tracking blocked IPs and temporarily locked user accounts.
 */
public final class SecurityState {
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();
    private final Map<String, Instant> lockedUsers = new ConcurrentHashMap<>();

    public boolean blockIp(String ip) {
        if (ip == null || ip.isBlank()) return false;
        return blockedIps.add(ip.trim());
    }

    public boolean unblockIp(String ip) {
        if (ip == null || ip.isBlank()) return false;
        return blockedIps.remove(ip.trim());
    }

    public boolean isIpBlocked(String ip) {
        if (ip == null) return false;
        return blockedIps.contains(ip.trim());
    }

    public boolean lockUser(String username, int seconds) {
        if (username == null || username.isBlank() || seconds < 1 || seconds > 3600) return false;
        String user = username.trim().toLowerCase(Locale.ROOT);
        Instant until = Instant.now().plusSeconds(seconds);
        lockedUsers.put(user, until);
        return true;
    }

    public boolean unlockUser(String username) {
        if (username == null) return false;
        return lockedUsers.remove(username.trim().toLowerCase(Locale.ROOT)) != null;
    }

    public boolean isUserLocked(String username) {
        if (username == null) return false;
        String user = username.trim().toLowerCase(Locale.ROOT);
        Instant expiry = lockedUsers.get(user);
        if (expiry == null) return false;
        if (Instant.now().isAfter(expiry)) {
            lockedUsers.remove(user, expiry);
            return false;
        }
        return true;
    }

    public Instant getLockExpiry(String username) {
        if (username == null) return null;
        String user = username.trim().toLowerCase(Locale.ROOT);
        if (isUserLocked(user)) {
            return lockedUsers.get(user);
        }
        return null;
    }

    public Set<String> getBlockedIps() {
        return Collections.unmodifiableSet(new HashSet<>(blockedIps));
    }

    public Map<String, Instant> getLockedUsers() {
        // Purge expired locks
        Instant now = Instant.now();
        lockedUsers.entrySet().removeIf(e -> now.isAfter(e.getValue()));
        return Collections.unmodifiableMap(new HashMap<>(lockedUsers));
    }

    public void reset() {
        blockedIps.clear();
        lockedUsers.clear();
    }
}
