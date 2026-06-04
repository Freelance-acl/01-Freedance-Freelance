package com.team01.freelance.proposal.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class CacheKeyUtil {

    private static final String SERVICE = "proposal-service";

    private CacheKeyUtil() {
    }

    public static String featureKey(String featureId, String paramHash) {
        return SERVICE + "::" + featureId + "::" + paramHash;
    }

    public static String entityKey(String entity, Long id) {
        return SERVICE + "::" + entity + "::" + id;
    }

    public static String hashParams(Map<String, ?> params) {
        String canonical = new TreeMap<>(params).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        return sha256(canonical);
    }

    public static String hashBody(String body) {
        return sha256(body == null ? "" : body);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
