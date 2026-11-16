package com.demobank;

import com.demobank.auth.PasswordHasher;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PasswordHasherTest {

    @Test
    void testSamePasswordAndSaltYieldSameHash() {
        String h1 = PasswordHasher.hash("Wonder@2024", "salt_alice");
        String h2 = PasswordHasher.hash("Wonder@2024", "salt_alice");
        assertNotNull(h1);
        assertEquals(h1, h2);
        assertEquals(64, h1.length());
    }

    @Test
    void testDifferentSaltsYieldDifferentHashes() {
        String h1 = PasswordHasher.hash("Wonder@2024", "salt_alice");
        String h2 = PasswordHasher.hash("Wonder@2024", "salt_bob");
        assertNotEquals(h1, h2);
    }

    @Test
    void testNullArgsThrowException() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null, "salt"));
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash("pass", null));
    }
}
