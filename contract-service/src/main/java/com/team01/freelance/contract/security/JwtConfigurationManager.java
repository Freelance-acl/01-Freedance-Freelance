package com.team01.freelance.contract.security;

public final class JwtConfigurationManager {

    private static final String DEFAULT_SECRET = "Y29udHJhY3Qtc2VydmljZS1tczItc2VjcmV0LWtleS0zMg==";
    private static final long DEFAULT_EXPIRATION_MS = 86_400_000L;

    private static volatile JwtConfigurationManager instance;

    private volatile String secret;
    private volatile long expirationMs;

    private JwtConfigurationManager() {
        this.secret = DEFAULT_SECRET;
        this.expirationMs = DEFAULT_EXPIRATION_MS;
    }

    public static JwtConfigurationManager getInstance() {
        if (instance == null) {
            synchronized (JwtConfigurationManager.class) {
                if (instance == null) {
                    instance = new JwtConfigurationManager();
                }
            }
        }
        return instance;
    }

    public static void initConfig(String secret, long expirationMs) {
        JwtConfigurationManager manager = getInstance();
        if (secret != null && !secret.isBlank()) {
            manager.secret = secret;
        }
        if (expirationMs > 0) {
            manager.expirationMs = expirationMs;
        }
    }

    public String getSecret() {
        return secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
