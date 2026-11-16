package com.demobank;

import com.demobank.auth.SessionStore;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {

    @Test
    void testSessionLifecycle() {
        SessionStore store = new SessionStore();
        String token = store.createSession("alice");
        assertNotNull(token);
        assertEquals(32, token.length());

        Optional<String> user = store.getUsername(token);
        assertTrue(user.isPresent());
        assertEquals("alice", user.get());

        // Invalidate via logout
        boolean loggedOut = store.invalidate(token);
        assertTrue(loggedOut);

        // Verification after logout
        Optional<String> afterLogout = store.getUsername(token);
        assertTrue(afterLogout.isEmpty());
    }

    @Test
    void testInvalidTokenReturnsEmpty() {
        SessionStore store = new SessionStore();
        assertTrue(store.getUsername("invalid-token").isEmpty());
        assertTrue(store.getUsername(null).isEmpty());
    }
}
