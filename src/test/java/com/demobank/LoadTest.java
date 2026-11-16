package com.demobank;

import com.demobank.account.AccountRepository;
import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.logging.AuthLogEntry;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class LoadTest {
    private Path tempLog;
    private Path tempCsv;
    private AuthLogger logger;
    private AuthService authService;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("load_test_log", ".log");
        tempCsv = Files.createTempFile("load_test_users", ".csv");
        Files.deleteIfExists(tempCsv);

        logger = new AuthLogger(tempLog.toString());
        UserRepository userRepo = new UserRepository(tempCsv.toString());
        SecurityState securityState = new SecurityState();
        SessionStore sessionStore = new SessionStore();
        authService = new AuthService(userRepo, securityState, sessionStore, logger);
    }

    @AfterEach
    void tearDown() throws IOException {
        logger.shutdown();
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(tempCsv);
    }

    @Test
    void testHighThroughput200RpsBenchmark() throws Exception {
        int totalRequests = 4000; // 200 req/sec over 20 concurrent workers
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        int perThread = totalRequests / threads;
        AtomicInteger completed = new AtomicInteger();

        long startTime = System.currentTimeMillis();
        for (int t = 0; t < threads; t++) {
            final int tid = t;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < perThread; i++) {
                        String user = (i % 5 == 0) ? "alice" : "admin";
                        String pass = (i % 2 == 0) ? "Wonder@2024" : "wrong";
                        authService.login(user, pass, "203.0.113." + (tid + 1));
                        completed.incrementAndGet();
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(20, TimeUnit.SECONDS));
        long duration = System.currentTimeMillis() - startTime;
        pool.shutdown();

        double rps = (completed.get() * 1000.0) / Math.max(1, duration);
        System.out.printf("Load benchmark completed: %d requests in %d ms (%.1f requests/sec)%n",
            completed.get(), duration, rps);

        // Verification: ensure logger caught up
        Thread.sleep(800);

        Gson gson = new Gson();
        int validLines = 0;
        try (BufferedReader reader = Files.newBufferedReader(tempLog)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    AuthLogEntry entry = gson.fromJson(line, AuthLogEntry.class);
                    assertNotNull(entry.ts());
                    validLines++;
                }
            }
        }
        assertEquals(totalRequests, validLines);
        assertTrue(rps >= 200, "Throughput should exceed 200 req/sec");
    }
}
