package com.demobank;

import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {
    private Path tempLog;
    private Path tempCsv;
    private AuthLogger logger;
    private AuthService authService;
    private SecurityState securityState;
    private SessionStore sessionStore;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("auth_svc_log", ".log");
        tempCsv = Files.createTempFile("users_test", ".csv");
        Files.deleteIfExists(tempCsv); // Allow UserRepository to seed defaults

        logger = new AuthLogger(tempLog.toString());
        UserRepository userRepo = new UserRepository(tempCsv.toString());
        securityState = new SecurityState();
        sessionStore = new SessionStore();
        authService = new AuthService(userRepo, securityState, sessionStore, logger);
    }

    @AfterEach
    void tearDown() throws IOException {
        logger.shutdown();
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(tempCsv);
    }

    @Test
    void testCheckOrderAndAllFiveOutcomes() {
        // 1. Blocked IP check comes first
        securityState.blockIp("203.0.113.50");
        AuthService.LoginResult r1 = authService.login("alice", "Wonder@2024", "203.0.113.50");
        assertEquals(403, r1.status());
        assertEquals("IP_BLOCKED", r1.reason());

        // 2. Unknown user check comes second (from non-blocked IP)
        AuthService.LoginResult r2 = authService.login("nonexistent", "secret", "192.0.2.1");
        assertEquals(401, r2.status());
        assertEquals("UNKNOWN_USER", r2.reason());

        // 3. Locked user check comes third
        securityState.lockUser("alice", 300);
        AuthService.LoginResult r3 = authService.login("alice", "Wonder@2024", "192.0.2.1");
        assertEquals(423, r3.status());
        assertEquals("ACCOUNT_LOCKED", r3.reason());
        securityState.unlockUser("alice");

        // 4. Bad password check comes fourth
        AuthService.LoginResult r4 = authService.login("alice", "WrongPass!", "192.0.2.1");
        assertEquals(401, r4.status());
        assertEquals("BAD_PASSWORD", r4.reason());

        // 5. Success comes fifth
        AuthService.LoginResult r5 = authService.login("alice", "Wonder@2024", "192.0.2.1");
        assertEquals(200, r5.status());
        assertEquals("NONE", r5.reason());
        assertNotNull(r5.token());
        assertEquals(32, r5.token().length());

        // Malformed check
        AuthService.LoginResult r6 = authService.login("", "pass", "192.0.2.1");
        assertEquals(400, r6.status());
    }
}
