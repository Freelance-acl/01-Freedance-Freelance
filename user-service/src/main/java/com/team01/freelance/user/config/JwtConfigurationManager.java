package com.team01.freelance.user.config;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * DP-5 Singleton — holds JWT signing configuration. Not a Spring bean.
 */
public final class JwtConfigurationManager {

    private static volatile JwtConfigurationManager instance;

    private final String secret;
    private final long expiration;
    private final SecretKey signingKey;

    private JwtConfigurationManager(String secret, long expiration) {
        this.secret = secret;
        this.expiration = expiration;
        this.signingKey = buildSigningKey(secret);
    }

    public static JwtConfigurationManager getInstance() {
        JwtConfigurationManager current = instance;
        if (current == null) {
            synchronized (JwtConfigurationManager.class) {
                current = instance;
                if (current == null) {
                    throw new IllegalStateException(
                            "JwtConfigurationManager is not configured; ensure jwt.secret is set");
                }
            }
        }
        return current;
    }

    public static synchronized void configure(String secret, long expiration) {
        instance = new JwtConfigurationManager(secret, expiration);
    }

    /** Test-only reset without Spring context. */
    public static synchronized void resetForTests(String secret, long expiration) {
        configure(secret, expiration);
    }

    public String getSecret() {
        return secret;
    }

    public long getExpiration() {
        return expiration;
    }

    public SecretKey getSigningKey() {
        return signingKey;
    }

    private static SecretKey buildSigningKey(String base64Secret) {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException(
                    "jwt.secret is not set; provide JWT_SECRET (Base64, at least 32 bytes when decoded)");
        }
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least 32 bytes (256 bits) for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
