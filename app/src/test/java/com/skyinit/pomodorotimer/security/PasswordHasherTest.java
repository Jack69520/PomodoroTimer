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
        assertTrue(PasswordHasher.verifyPassword("TestPass1", hash));
        assertFalse(PasswordHasher.verifyPassword("WrongPass1", hash));
    }

    @Test
    public void extractSaltHex_fromPbkdf2Hash() {
        String hash = PasswordHasher.hashPassword("SaltTest1");
        String salt = PasswordHasher.extractSaltHex(hash);
        assertNotNull(salt);
        assertTrue(salt.length() >= 32);
    }
}
