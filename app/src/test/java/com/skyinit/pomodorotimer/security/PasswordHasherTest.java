package com.skyinit.pomodorotimer.security;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class PasswordHasherTest {

    @Test
    public void pbkdf2Hash_verify_succeeds() {
        String hash = PasswordHasher.hashPassword("TestPass1");
        assertNotNull(hash);
        assertTrue(hash.startsWith(PasswordHasher.PBKDF2_PREFIX));
        assertTrue(PasswordHasher.verifyPassword("TestPass1", hash, null));
        assertFalse(PasswordHasher.verifyPassword("WrongPass1", hash, null));
    }

    @Test
    public void legacySha256_verify_succeeds() {
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hashPasswordLegacy("LegacyPass1", salt);
        assertTrue(PasswordHasher.isLegacyHash(hash));
        assertTrue(PasswordHasher.verifyPassword("LegacyPass1", hash, salt));
        assertFalse(PasswordHasher.verifyPassword("WrongPass1", hash, salt));
    }

    @Test
    public void pbkdf2Hash_isNotLegacy() {
        String hash = PasswordHasher.hashPassword("AnyPass1");
        assertFalse(PasswordHasher.isLegacyHash(hash));
    }

    @Test
    public void extractSaltHex_fromPbkdf2Hash() {
        String hash = PasswordHasher.hashPassword("SaltTest1");
        String salt = PasswordHasher.extractSaltHex(hash);
        assertNotNull(salt);
        assertTrue(salt.length() >= 32);
    }
}
