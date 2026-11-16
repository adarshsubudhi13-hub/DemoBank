package com.demobank.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-backed user repository providing full CRUD operations.
 * Demonstrates:
 * - Object-Oriented Programming (Encapsulation, Polymorphism)
 * - JDBC PreparedStatement, ResultSet, and try-with-resources
 * - Advanced Java Collections (Optional, List) and Exception Handling
 */
public class JdbcUserRepository {
    public record DbUser(String username, String salt, String sha256hash,
                         String fullName, String accountId, boolean locked, long createdAt) {}

    private final DatabaseManager dbManager;

    public JdbcUserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Create: Inserts a user into the database.
     */
    public boolean save(DbUser user) throws SQLException {
        String sql = """
            INSERT INTO users (username, salt, sha256hash, full_name, account_id, is_locked, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(username) DO UPDATE SET
            sha256hash = excluded.sha256hash,
            full_name = excluded.full_name,
            account_id = excluded.account_id,
            is_locked = excluded.is_locked;
            """;
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.username().toLowerCase());
            ps.setString(2, user.salt());
            ps.setString(3, user.sha256hash());
            ps.setString(4, user.fullName());
            ps.setString(5, user.accountId());
            ps.setInt(6, user.locked() ? 1 : 0);
            ps.setLong(7, user.createdAt());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Read: Retrieve a user by username.
     */
    public Optional<DbUser> findByUsername(String username) throws SQLException {
        if (username == null) return Optional.empty();
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        }
    }

    /**
     * Read: Retrieve all users.
     */
    public List<DbUser> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY username ASC";
        List<DbUser> list = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /**
     * Update: Update user lock status.
     */
    public boolean updateLockStatus(String username, boolean locked) throws SQLException {
        String sql = "UPDATE users SET is_locked = ? WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, locked ? 1 : 0);
            ps.setString(2, username.toLowerCase());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Delete: Remove a user by username.
     */
    public boolean deleteByUsername(String username) throws SQLException {
        String sql = "DELETE FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.toLowerCase());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Count total registered users in JDBC database.
     */
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private DbUser mapRow(ResultSet rs) throws SQLException {
        return new DbUser(
                rs.getString("username"),
                rs.getString("salt"),
                rs.getString("sha256hash"),
                rs.getString("full_name"),
                rs.getString("account_id"),
                rs.getInt("is_locked") == 1,
                rs.getLong("created_at")
        );
    }
}
