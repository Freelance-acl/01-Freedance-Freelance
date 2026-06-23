package com.team01.freelance.proposal.config.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Loads Spring {@code jwt.*} properties into {@link JwtConfigurationManager} at startup.
 */
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
