package com.skyinit.pomodorotimer.domain.account;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.Predicate;

/**
 * 12 位账户 ID：4 位分钟时间戳 + 7 位随机 + 1 位校验（权重 1,3 交替，前 11 位加权和 mod 10）。
 */
public final class UserIdGenerator {

    public static final int USER_ID_LENGTH = 12;
    private static final int MAX_ATTEMPTS = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private UserIdGenerator() {
    }

    @NonNull
    public static String generateUnique(@NonNull Predicate<String> exists) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String candidate = generateOnce();
            if (!exists.test(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to allocate unique user id after " + MAX_ATTEMPTS + " attempts");
    }

    @NonNull
    public static String generateOnce() {
        long minuteBucket = (System.currentTimeMillis() / 1000L) / 60L;
        String timePart = String.format(Locale.US, "%04d", (int) (minuteBucket % 10000L));
        StringBuilder randomPart = new StringBuilder(7);
        for (int i = 0; i < 7; i++) {
            randomPart.append(RANDOM.nextInt(10));
        }
        String first11 = timePart + randomPart;
        return first11 + computeCheckDigit(first11);
    }

    public static boolean isValidFormat(@Nullable String userId) {
        if (userId == null || userId.length() != USER_ID_LENGTH) {
            return false;
        }
        for (int i = 0; i < USER_ID_LENGTH; i++) {
            char c = userId.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        String first11 = userId.substring(0, 11);
        char expected = computeCheckDigit(first11);
        return userId.charAt(11) == expected;
    }

    public static char computeCheckDigit(@NonNull String first11) {
        if (first11.length() != 11) {
            throw new IllegalArgumentException("first11 must be 11 digits");
        }
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            char c = first11.charAt(i);
            if (c < '0' || c > '9') {
                throw new IllegalArgumentException("first11 must be digits");
            }
            int weight = (i % 2 == 0) ? 1 : 3;
            sum += (c - '0') * weight;
        }
        return (char) ('0' + (sum % 10));
    }
}
