package com.demobank;

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

public class ConcurrencyTest {
    private Path tempLog;
    private Path tempCsv;
    private AuthLogger logger;
    private AuthService authService;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("concurrency_log", ".log");
        tempCsv = Files.createTempFile("concurrency_users", ".csv");
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
    void test20ThreadsX500LoginsExactCountersAndValidJson() throws Exception {
        int threads = 20;
        int loginsPerThread = 500;
        int totalExpected = threads * loginsPerThread;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            pool.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < loginsPerThread; i++) {
                        boolean willSucceed = (i % 2 == 0);
                        String user = "alice";
                        String pass = willSucceed ? "Wonder@2024" : "WrongPass";
                        String ip = "203.0.113." + (threadId + 1);

                        AuthService.LoginResult res = authService.login(user, pass, ip);
                        if (res.status() == 200) {
                            successCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS));
        pool.shutdown();

        // Exact counters check
        assertEquals(totalExpected, authService.getAttempts());
        assertEquals(failCount.get(), authService.getFailures());
        assertEquals(totalExpected, successCount.get() + failCount.get());

        // Wait for single logger thread to drain queue
        Thread.sleep(1000);

        // Verify every log line is valid JSON
        Gson gson = new Gson();
        int lineCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(tempLog)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    AuthLogEntry entry = gson.fromJson(line, AuthLogEntry.class);
                    assertNotNull(entry.ts());
                    assertNotNull(entry.ip());
                    assertNotNull(entry.username());
                    lineCount++;
                }
            }
        }
        assertEquals(totalExpected, lineCount);
    }
}
