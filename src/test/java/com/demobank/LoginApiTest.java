package com.demobank;

import com.demobank.account.AccountRepository;
import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.logging.AuthLogEntry;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import com.demobank.server.DemoBankServer;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class LoginApiTest {
    private Path tempLog;
    private Path tempCsv;
    private Path tempJson;
    private DemoBankServer server;
    private SecurityState securityState;
    private AuthLogger authLogger;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("login_api_log", ".log");
        tempCsv = Files.createTempFile("login_users", ".csv");
        tempJson = Files.createTempFile("login_accounts", ".json");
        Files.deleteIfExists(tempCsv);
        Files.deleteIfExists(tempJson);

        UserRepository userRepo = new UserRepository(tempCsv.toString());
        AccountRepository accRepo = new AccountRepository(tempJson.toString());
        securityState = new SecurityState();
        SessionStore sessionStore = new SessionStore();
        authLogger = new AuthLogger(tempLog.toString());
        AuthService authService = new AuthService(userRepo, securityState, sessionStore, authLogger);

        // Ephemeral port: find available port by binding 0
        server = new DemoBankServer(0, authService, userRepo, accRepo, sessionStore, securityState, authLogger);
        server.start();
        port = server.getPort();

        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.stop();
        authLogger.shutdown();
        Files.deleteIfExists(tempLog);
        Files.deleteIfExists(tempCsv);
        Files.deleteIfExists(tempJson);
    }

    @Test
    void testAllLoginPathsAndExactLogLines() throws Exception {
        String baseUrl = "http://127.0.0.1:" + port;

        // 1. Success (200)
        HttpResponse<String> r200 = postLogin(baseUrl, "alice", "Wonder@2024", "192.0.2.1");
        assertEquals(200, r200.statusCode());
        assertTrue(r200.headers().firstValue("Set-Cookie").orElse("").contains("SID="));

        // 2. Bad password (401)
        HttpResponse<String> r401 = postLogin(baseUrl, "alice", "BadPass123", "192.0.2.2");
        assertEquals(401, r401.statusCode());

        // 3. Unknown user (401)
        HttpResponse<String> rUnknown = postLogin(baseUrl, "nobody", "BadPass123", "192.0.2.3");
        assertEquals(401, rUnknown.statusCode());

        // 4. Locked account (423)
        securityState.lockUser("alice", 60);
        HttpResponse<String> r423 = postLogin(baseUrl, "alice", "Wonder@2024", "192.0.2.4");
        assertEquals(423, r423.statusCode());
        securityState.unlockUser("alice");

        // 5. Blocked IP (403)
        securityState.blockIp("203.0.113.99");
        HttpResponse<String> r403 = postLogin(baseUrl, "alice", "Wonder@2024", "203.0.113.99");
        assertEquals(403, r403.statusCode());

        // 6. Malformed JSON or empty username (400)
        HttpResponse<String> r400 = postLogin(baseUrl, "", "", "192.0.2.5");
        assertEquals(400, r400.statusCode());

        Thread.sleep(400);

        List<String> logLines = Files.readAllLines(tempLog);
        // Note: malformed (400) is not logged per specification
        assertEquals(5, logLines.size());

        Gson gson = new Gson();
        AuthLogEntry l1 = gson.fromJson(logLines.get(0), AuthLogEntry.class);
        assertEquals(200, l1.status());
        assertEquals("NONE", l1.reason());
        assertTrue(l1.success());

        AuthLogEntry l2 = gson.fromJson(logLines.get(1), AuthLogEntry.class);
        assertEquals(401, l2.status());
        assertEquals("BAD_PASSWORD", l2.reason());
        assertFalse(l2.success());

        AuthLogEntry l3 = gson.fromJson(logLines.get(2), AuthLogEntry.class);
        assertEquals(401, l3.status());
        assertEquals("UNKNOWN_USER", l3.reason());

        AuthLogEntry l4 = gson.fromJson(logLines.get(3), AuthLogEntry.class);
        assertEquals(423, l4.status());
        assertEquals("ACCOUNT_LOCKED", l4.reason());

        AuthLogEntry l5 = gson.fromJson(logLines.get(4), AuthLogEntry.class);
        assertEquals(403, l5.status());
        assertEquals("IP_BLOCKED", l5.reason());
    }

    private HttpResponse<String> postLogin(String baseUrl, String user, String pass, String ip) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}";
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/login"))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", ip)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }
}
