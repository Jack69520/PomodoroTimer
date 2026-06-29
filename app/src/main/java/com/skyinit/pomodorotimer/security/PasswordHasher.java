package com.skyinit.pomodorotimer.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * 密码哈希：默认 PBKDF2-HMAC-SHA256；兼容旧版单次 SHA-256 并在登录时升级。
 */
public final class PasswordHasher {

    public static final String PBKDF2_PREFIX = "pbkdf2:";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int PBKDF2_KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** 生成新密码哈希（PBKDF2）。 */
    public static String hashPassword(String password) {
        if (password == null) {
            password = "";
        }
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH_BITS);
        return encodePbkdf2(PBKDF2_ITERATIONS, salt, hash);
    }

    /** 从 PBKDF2 存储串中提取盐（供 User.passwordSalt 字段同步）。 */
    public static String extractSaltHex(String storedHash) {
        if (storedHash == null || !storedHash.startsWith(PBKDF2_PREFIX)) {
            return null;
        }
        String[] parts = storedHash.split(":");
        return parts.length >= 4 ? parts[2] : null;
    }

    public static boolean isLegacyHash(String storedHash) {
        return storedHash != null && !storedHash.startsWith(PBKDF2_PREFIX);
    }

    public static boolean verifyPassword(String password, String storedHash, String legacySalt) {
        if (storedHash == null) {
            return false;
        }
        if (storedHash.startsWith(PBKDF2_PREFIX)) {
            return verifyPbkdf2(password, storedHash);
        }
        if (legacySalt == null) {
            return false;
        }
        return verifyLegacySha256(password, legacySalt, storedHash);
    }

    /** @deprecated 仅用于兼容旧数据；新密码请使用 {@link #hashPassword(String)}。 */
    @Deprecated
    public static String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    /** @deprecated 仅用于兼容旧数据。 */
    @Deprecated
    public static String hashPasswordLegacy(String password, String saltHex) {
        if (password == null) {
            password = "";
        }
        if (saltHex == null) {
            saltHex = "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((saltHex + password).getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static boolean verifyPbkdf2(String password, String storedHash) {
        String[] parts = storedHash.split(":");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = fromHex(parts[2]);
            byte[] expected = fromHex(parts[3]);
            byte[] actual = pbkdf2(
                    password == null ? new char[0] : password.toCharArray(),
                    salt,
                    iterations,
                    expected.length * 8
            );
            return constantTimeEquals(expected, actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean verifyLegacySha256(String password, String saltHex, String expectedHash) {
        String actual = hashPasswordLegacy(password, saltHex);
        return constantTimeEquals(
                expectedHash.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actual.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("PBKDF2 unavailable", e);
        }
    }

    private static String encodePbkdf2(int iterations, byte[] salt, byte[] hash) {
        return PBKDF2_PREFIX + iterations + ":" + toHex(salt) + ":" + toHex(hash);
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                builder.append('0');
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    private static byte[] fromHex(String hex) {
        int length = hex.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            data[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return data;
    }
}
