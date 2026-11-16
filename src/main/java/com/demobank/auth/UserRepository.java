package com.demobank.auth;

import com.demobank.config.Config;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for user credentials and metadata backed by users.csv.
 */
public final class UserRepository {
    public record UserRecord(String username, String salt, String sha256hash, String fullName, String accountId) {}

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();
    private final String csvPath;

    public UserRepository() {
        this(Config.getUsersPath());
    }

    public UserRepository(String csvPath) {
        this.csvPath = csvPath;
        initUsers();
    }

    public Optional<UserRecord> findByUsername(String username) {
        if (username == null) return Optional.empty();
        return Optional.ofNullable(users.get(username.toLowerCase(Locale.ROOT)));
    }

    public boolean userExists(String username) {
        return findByUsername(username).isPresent();
    }

    private void initUsers() {
        Path path = Paths.get(csvPath);
        if (!Files.exists(path)) {
            seedDefaults(path);
        }
        loadFromCsv(path);
    }

    private void seedDefaults(Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
            List<String> lines = new ArrayList<>();
            lines.add("username,salt,sha256hash,fullName,accountId");
            String[][] seeds = {
                {"alice", "salt_alice", "Wonder@2024", "Alice Johnson", "ACC-1001"},
                {"bob", "salt_bob", "Builder#77", "Bob Smith", "ACC-1002"},
                {"charlie", "salt_charlie", "Choc0late!", "Charlie Brown", "ACC-1003"},
                {"admin", "salt_admin", "Adm1n$ecure", "Admin User", "ACC-1004"},
                {"guest", "salt_guest", "guest123", "Guest User", "ACC-1005"},
                {"adarsh", "salt_adarsh", "Adarsh@123", "Adarsh Subudhi", "ACC-1006"}
            };
            for (String[] s : seeds) {
                String hash = PasswordHasher.hash(s[2], s[1]);
                lines.add(String.join(",", s[0], s[1], hash, s[3], s[4]));
            }
            Files.write(path, lines);
        } catch (IOException e) {
            System.err.println("Failed to seed users.csv: " + e.getMessage());
        }
    }

    private void loadFromCsv(Path path) {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",", -1);
                if (p.length >= 5) {
                    users.put(p[0].trim().toLowerCase(Locale.ROOT),
                        new UserRecord(p[0].trim(), p[1].trim(), p[2].trim(), p[3].trim(), p[4].trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read users.csv: " + e.getMessage());
        }
    }
}
