package com.demobank.logging;

/**
 * Immutable log entry representing an authentication event in auth.log.
 */
public record AuthLogEntry(
    String ts,
    String ip,
    String username,
    boolean success,
    String reason,
    int status
) {}
