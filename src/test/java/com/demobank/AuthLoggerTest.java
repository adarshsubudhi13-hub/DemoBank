package com.demobank;

import com.demobank.logging.AuthLogEntry;
import com.demobank.logging.AuthLogger;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class AuthLoggerTest {
    private Path tempLog;
    private AuthLogger logger;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("auth_test", ".log");
        logger = new AuthLogger(tempLog.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        logger.shutdown();
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(Path.of(tempLog.toString() + ".bak"));
    }

    @Test
    void testLinesAreValidJsonAndInOrder() throws Exception {
        AuthLogEntry e1 = new AuthLogEntry(Instant.now().toString(), "203.0.113.1", "user1", false, "BAD_PASSWORD", 401);
        AuthLogEntry e2 = new AuthLogEntry(Instant.now().toString(), "203.0.113.2", "user2", true, "NONE", 200);

        logger.log(e1);
        logger.log(e2);

        Thread.sleep(300);

        List<String> lines = Files.readAllLines(tempLog);
        assertEquals(2, lines.size());

        Gson gson = new Gson();
        AuthLogEntry parsed1 = gson.fromJson(lines.get(0), AuthLogEntry.class);
        assertEquals("user1", parsed1.username());
        assertEquals("BAD_PASSWORD", parsed1.reason());
        assertEquals(401, parsed1.status());

        AuthLogEntry parsed2 = gson.fromJson(lines.get(1), AuthLogEntry.class);
        assertEquals("user2", parsed2.username());
        assertEquals("NONE", parsed2.reason());
        assertEquals(200, parsed2.status());
    }
}
