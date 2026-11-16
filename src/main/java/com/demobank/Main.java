package com.demobank;

import com.demobank.account.AccountRepository;
import com.demobank.auth.AuthService;
import com.demobank.auth.SessionStore;
import com.demobank.auth.UserRepository;
import com.demobank.config.Config;
import com.demobank.logging.AuthLogger;
import com.demobank.security.SecurityState;
import com.demobank.server.DemoBankServer;
import java.io.File;
import java.io.IOException;

/**
 * Bootstrap entry point for DemoBank victim service.
 */
public final class Main {
    public static void main(String[] args) throws IOException {
        new File("data").mkdirs();
        Config.load("config.properties");

        UserRepository userRepo = new UserRepository();
        AccountRepository accountRepo = new AccountRepository();
        SecurityState securityState = new SecurityState();
        SessionStore sessionStore = new SessionStore();
        AuthLogger authLogger = new AuthLogger();
        AuthService authService = new AuthService(userRepo, securityState, sessionStore, authLogger);

        int port = Config.getPort();
        DemoBankServer server = new DemoBankServer(port, authService, userRepo, accountRepo, sessionStore, securityState, authLogger);
        server.start();

        // Launch Java Swing GUI by default unless explicitly running headless
        boolean headless = java.awt.GraphicsEnvironment.isHeadless();
        for (String arg : args) {
            if ("--headless".equalsIgnoreCase(arg) || "--server-only".equalsIgnoreCase(arg)) {
                headless = true;
                break;
            }
        }
        if (!headless) {
            try {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    com.demobank.gui.DemoBankSwingApp gui = new com.demobank.gui.DemoBankSwingApp();
                    gui.setVisible(true);
                });
            } catch (Throwable t) {
                System.err.println("Note: Swing GUI initialization skipped: " + t.getMessage());
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down DemoBank...");
            server.stop();
            authLogger.shutdown();
        }));
    }
}
