package com.team01.freelance.job.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtConfigurationBootstrap implements InitializingBean {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public void afterPropertiesSet() {
        JwtConfigurationManager.configure(secret, expiration);
    }
}
