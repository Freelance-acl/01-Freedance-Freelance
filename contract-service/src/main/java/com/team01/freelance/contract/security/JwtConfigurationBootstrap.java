package com.team01.freelance.contract.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfigurationBootstrap {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @PostConstruct
    void initialize() {
        JwtConfigurationManager.initConfig(secret, expiration);
    }
}
