package com.demobank;

import com.demobank.account.AccountRepository;
import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import com.demobank.server.DemoBankServer;
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
import static org.junit.jupiter.api.Assertions.*;

public class AdminApiTest {
    private Path tempLog;
    private Path tempCsv;
    private Path tempJson;
    private DemoBankServer server;
    private SecurityState securityState;
    private AuthLogger authLogger;
    private AuthService authService;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() throws IOException {
        tempLog = Files.createTempFile("admin_api_log", ".log");
        tempCsv = Files.createTempFile("admin_users", ".csv");
        tempJson = Files.createTempFile("admin_accounts", ".json");
        Files.deleteIfExists(tempCsv);
        Files.deleteIfExists(tempJson);

        UserRepository userRepo = new UserRepository(tempCsv.toString());
        AccountRepository accRepo = new AccountRepository(tempJson.toString());
        securityState = new SecurityState();
        SessionStore sessionStore = new SessionStore();
        authLogger = new AuthLogger(tempLog.toString());
        authService = new AuthService(userRepo, securityState, sessionStore, authLogger);

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
        Files.deleteIfExists(Path.of(tempLog.toString() + ".bak"));
        Files.deleteIfExists(tempCsv);
        Files.deleteIfExists(tempJson);
    }

    @Test
    void testTokenAuthenticationAndAdminActions() throws Exception {
        String baseUrl = "http://127.0.0.1:" + port;

        // 1. Missing token gives 401
        HttpRequest reqNoToken = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/admin/status"))
            .GET()
            .build();
        assertEquals(401, client.send(reqNoToken, HttpResponse.BodyHandlers.ofString()).statusCode());

        // 2. Block IP via admin
        HttpRequest blockReq = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/admin/block"))
            .header("X-Admin-Token", "demo-admin-token")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"ip\":\"203.0.113.88\"}"))
            .build();
        HttpResponse<String> blockResp = client.send(blockReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, blockResp.statusCode());
        assertTrue(blockResp.body().contains("\"changed\":true"));

        // Login from blocked IP returns 403
        HttpRequest loginBlocked = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/login"))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", "203.0.113.88")
            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"alice\",\"password\":\"Wonder@2024\"}"))
            .build();
        assertEquals(403, client.send(loginBlocked, HttpResponse.BodyHandlers.ofString()).statusCode());

        // 3. Lock user via admin
        HttpRequest lockReq = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/admin/lock-user"))
            .header("X-Admin-Token", "demo-admin-token")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"bob\",\"seconds\":60}"))
            .build();
        HttpResponse<String> lockResp = client.send(lockReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, lockResp.statusCode());
        assertTrue(lockResp.body().contains("\"changed\":true"));

        // Login to locked user returns 423
        HttpRequest loginLocked = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/login"))
            .header("Content-Type", "application/json")
            .header("X-Forwarded-For", "192.0.2.1")
            .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"bob\",\"password\":\"Builder#77\"}"))
            .build();
        assertEquals(423, client.send(loginLocked, HttpResponse.BodyHandlers.ofString()).statusCode());

        // 4. Reset admin endpoint
        HttpRequest resetReq = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/admin/reset"))
            .header("X-Admin-Token", "demo-admin-token")
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();
        HttpResponse<String> resetResp = client.send(resetReq, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resetResp.statusCode());
        assertTrue(resetResp.body().contains("\"reset\":true"));

        // After reset, unblocked IP can now login successfully
        HttpResponse<String> loginAfterReset = client.send(loginBlocked, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginAfterReset.statusCode());
    }
}
