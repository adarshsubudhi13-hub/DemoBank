package com.demobank;

import com.demobank.auth.PasswordHasher;
import com.demobank.database.DatabaseManager;
import com.demobank.database.JdbcUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test verifying DemoBank JDBC user repository CRUD operations.
 */
class JdbcUserTest {

    private DatabaseManager dbManager;
    private JdbcUserRepository repository;
    private final String testDbPath = "data/test_demobank.db";

    @BeforeEach
    void setUp() {
        new File("data").mkdirs();
        new File(testDbPath).delete();
        dbManager = new DatabaseManager(testDbPath);
        repository = new JdbcUserRepository(dbManager);
    }

    @AfterEach
    void tearDown() {
        new File(testDbPath).delete();
    }

    @Test
    @DisplayName("Create (Insert) and Read (Select) user via JDBC")
    void testCreateAndReadUser() throws SQLException {
        String salt = "salt_adarsh";
        String hash = PasswordHasher.hash("Adarsh@123", salt);
        JdbcUserRepository.DbUser user = new JdbcUserRepository.DbUser(
                "adarsh", salt, hash, "Adarsh Subudhi", "ACC-1006", false, System.currentTimeMillis()
        );

        // 1. Create
        boolean saved = repository.save(user);
        assertTrue(saved);

        // 2. Read
        Optional<JdbcUserRepository.DbUser> fetched = repository.findByUsername("adarsh");
        assertTrue(fetched.isPresent());
        assertEquals("Adarsh Subudhi", fetched.get().fullName());
        assertEquals("ACC-1006", fetched.get().accountId());
        assertFalse(fetched.get().locked());

        // 3. Count
        assertEquals(1L, repository.count());
    }

    @Test
    @DisplayName("Update (Lock status) and Delete user via JDBC")
    void testUpdateAndDeleteUser() throws SQLException {
        String salt = "salt_alice";
        String hash = PasswordHasher.hash("Wonder@2024", salt);
        repository.save(new JdbcUserRepository.DbUser("alice", salt, hash, "Alice Johnson", "ACC-1001", false, System.currentTimeMillis()));

        // Update lock
        boolean updated = repository.updateLockStatus("alice", true);
        assertTrue(updated);

        Optional<JdbcUserRepository.DbUser> fetched = repository.findByUsername("alice");
        assertTrue(fetched.isPresent());
        assertTrue(fetched.get().locked());

        // Delete
        boolean deleted = repository.deleteByUsername("alice");
        assertTrue(deleted);
        assertEquals(0L, repository.count());
    }
}
