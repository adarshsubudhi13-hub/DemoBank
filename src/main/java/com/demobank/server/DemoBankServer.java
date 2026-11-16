package com.demobank.server;

import com.demobank.account.AccountRepository;
import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Embedded HTTP server for DemoBank running on 127.0.0.1 with a pool of 16 worker threads.
 */
public final class DemoBankServer {
    private final HttpServer server;
    private final int port;

    public DemoBankServer(int port, AuthService authService, UserRepository userRepo,
                          AccountRepository accountRepo, SessionStore sessionStore,
                          SecurityState securityState, AuthLogger authLogger) throws IOException {
        this.port = port;
        this.server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        this.server.setExecutor(Executors.newFixedThreadPool(16));

        StaticHandler staticHandler = new StaticHandler();
        this.server.createContext("/", staticHandler);
        this.server.createContext("/dashboard", staticHandler);
        this.server.createContext("/status.html", staticHandler);
        this.server.createContext("/css", staticHandler);
        this.server.createContext("/js", staticHandler);

        this.server.createContext("/api/login", new LoginHandler(authService));
        this.server.createContext("/api/logout", new LogoutHandler(sessionStore));

        AccountHandler accountHandler = new AccountHandler(authService, userRepo, accountRepo, sessionStore, securityState);
        this.server.createContext("/api/account", accountHandler);
        this.server.createContext("/api/stats", accountHandler);

        this.server.createContext("/admin", new AdminHandler(securityState, userRepo, authService, sessionStore, authLogger));
        this.server.createContext("/health", new HealthHandler());
    }

    public void start() {
        server.start();
        System.out.println("DemoBank running on http://127.0.0.1:" + getPort());
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }
}
