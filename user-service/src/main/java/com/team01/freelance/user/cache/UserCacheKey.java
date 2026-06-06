package com.team01.freelance.user.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public final class UserCacheKey {

    private UserCacheKey() {
    }

    public static String hash(Object... parts) {
        String joined = Arrays.stream(parts)
                .map(UserCacheKey::normalize)
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private static String normalize(Object value) {
        return value == null ? "<null>" : value.toString().trim();
    }
}
