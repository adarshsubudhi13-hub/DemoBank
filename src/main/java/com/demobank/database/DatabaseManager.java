package com.demobank.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages JDBC connections to DemoBank's SQLite database.
 * Demonstrates Singleton pattern, encapsulation, and robust JDBC connection management.
 */
public final class DatabaseManager {
    private static volatile DatabaseManager instance;
    private final String dbUrl;

    public DatabaseManager(String dbPath) {
        this.dbUrl = "jdbc:sqlite:" + dbPath;
        initDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    File dataDir = new File("data");
                    if (!dataDir.exists()) {
                        dataDir.mkdirs();
                    }
                    instance = new DatabaseManager("data/demobank.db");
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    private void initDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[WARN] SQLite JDBC Driver not found: " + e.getMessage());
        }

        String sqlUsers = """
            CREATE TABLE IF NOT EXISTS users (
                username TEXT PRIMARY KEY,
                salt TEXT NOT NULL,
                sha256hash TEXT NOT NULL,
                full_name TEXT NOT NULL,
                account_id TEXT NOT NULL,
                is_locked INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            );
            """;

        String sqlAudit = """
            CREATE TABLE IF NOT EXISTS audit_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                client_ip TEXT NOT NULL,
                username TEXT NOT NULL,
                action TEXT NOT NULL,
                status TEXT NOT NULL,
                reason TEXT
            );
            """;

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sqlUsers);
            stmt.execute(sqlAudit);
        } catch (SQLException e) {
            System.err.println("[ERROR] Failed to initialize DemoBank SQLite schema: " + e.getMessage());
        }
    }
}
