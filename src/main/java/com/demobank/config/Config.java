package com.demobank.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration holder for DemoBank with properties file loading and fallbacks.
 */
public final class Config {
    private static int port = 9090;
    private static String adminToken = "demo-admin-token";
    private static int sessionMinutes = 15;
    private static String authLogPath = "data/auth.log";
    private static String usersPath = "data/users.csv";
    private static String accountsPath = "data/accounts.json";

    public static void load(String filePath) {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(filePath)) {
            props.load(in);
            port = Integer.parseInt(props.getProperty("port", "9090"));
            adminToken = props.getProperty("adminToken", "demo-admin-token");
            sessionMinutes = Integer.parseInt(props.getProperty("sessionMinutes", "15"));
            authLogPath = props.getProperty("authLogPath", "data/auth.log");
            usersPath = props.getProperty("usersPath", "data/users.csv");
            accountsPath = props.getProperty("accountsPath", "data/accounts.json");
        } catch (IOException | NumberFormatException ignored) {}
    }

    public static int getPort() { return port; }
    public static String getAdminToken() { return adminToken; }
    public static int getSessionMinutes() { return sessionMinutes; }
    public static String getAuthLogPath() { return authLogPath; }
    public static String getUsersPath() { return usersPath; }
    public static String getAccountsPath() { return accountsPath; }
}
