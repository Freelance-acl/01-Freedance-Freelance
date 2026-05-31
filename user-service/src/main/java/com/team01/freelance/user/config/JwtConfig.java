package com.team01.freelance.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfig {

    
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    public String getSecret() {
        return secret;
    }

    public long getExpiration() {
        return expiration;
    }

    /** For unit tests; production values come from {@code application.yaml}. */
    public void setSecret(String secret) {
        this.secret = secret;
    }

    /** For unit tests; production values come from {@code application.yaml}. */
    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
