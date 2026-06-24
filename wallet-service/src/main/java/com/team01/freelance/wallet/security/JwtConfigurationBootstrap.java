package com.team01.freelance.wallet.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class JwtConfigurationBootstrap implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(JwtConfigurationBootstrap.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Override
    public void afterPropertiesSet() {
        log.info("JwtConfigurationBootstrap.afterPropertiesSet — configuring JwtConfigurationManager");
        JwtConfigurationManager.configure(secret, expiration);
        log.info("JwtConfigurationManager configured successfully");
    }

    // Safety net: re-configure after full context startup in case of initialization order issues
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (JwtConfigurationManager.isConfigured()) {
            log.info("JwtConfigurationManager already configured (ApplicationReadyEvent check passed)");
        } else {
            log.warn("JwtConfigurationManager was NOT configured at ApplicationReadyEvent — configuring now");
            JwtConfigurationManager.configure(secret, expiration);
        }
    }
}
