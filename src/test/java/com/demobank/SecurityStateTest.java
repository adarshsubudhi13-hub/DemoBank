package com.demobank;

import com.demobank.security.SecurityState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SecurityStateTest {
    private SecurityState state;

    @BeforeEach
    void setUp() {
        state = new SecurityState();
    }

    @Test
    void testBlockAndUnblock() {
        assertFalse(state.isIpBlocked("203.0.113.10"));
        assertTrue(state.blockIp("203.0.113.10"));
        assertTrue(state.isIpBlocked("203.0.113.10"));
        // Idempotent block returns false because state didn't change
        assertFalse(state.blockIp("203.0.113.10"));

        assertTrue(state.unblockIp("203.0.113.10"));
        assertFalse(state.isIpBlocked("203.0.113.10"));
        assertFalse(state.unblockIp("203.0.113.10"));
    }

    @Test
    void testLockAndLazyExpiry() throws InterruptedException {
        assertFalse(state.isUserLocked("alice"));
        assertTrue(state.lockUser("alice", 1)); // 1 second lock
        assertTrue(state.isUserLocked("alice"));

        Thread.sleep(1100);
        assertFalse(state.isUserLocked("alice"));
    }

    @Test
    void testUnlockUser() {
        state.lockUser("bob", 60);
        assertTrue(state.isUserLocked("bob"));
        assertTrue(state.unlockUser("bob"));
        assertFalse(state.isUserLocked("bob"));
    }

    @Test
    void testReset() {
        state.blockIp("198.51.100.5");
        state.lockUser("admin", 120);
        assertEquals(1, state.getBlockedIps().size());
        assertEquals(1, state.getLockedUsers().size());

        state.reset();
        assertEquals(0, state.getBlockedIps().size());
        assertEquals(0, state.getLockedUsers().size());
    }
}
