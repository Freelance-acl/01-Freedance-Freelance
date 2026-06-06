package com.team01.freelance.proposal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import com.team01.freelance.user.config.JwtConfigurationManager;
import com.team01.freelance.user.service.JwtService;

@Configuration
public class JwtServiceConfig {

    @Bean
    JwtService jwtService(
            @Value("${jwt.secret:cHJvcG9zYWwtc2VydmljZS10ZXN0LXNlY3JldC0zMi1ieXRlcw==}") String secret,
            @Value("${jwt.expiration:3600000}") long expiration) {
        JwtConfigurationManager.configure(secret, expiration);
        return new JwtService();
    }
}
