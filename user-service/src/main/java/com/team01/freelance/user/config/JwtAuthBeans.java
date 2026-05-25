package com.team01.freelance.user.config;

import com.team01.freelance.user.security.JwtAuthFilter;
import com.team01.freelance.user.service.CustomUserDetailsService;
import com.team01.freelance.user.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Shared JWT beans imported by every Freelance microservice.
 */
@Configuration
@ComponentScan(
        basePackages = "com.team01.freelance.user",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        JwtConfig.class,
                        JwtService.class,
                        JwtAuthFilter.class,
                        CustomUserDetailsService.class
                }),
        useDefaultFilters = false)
public class JwtAuthBeans {

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
