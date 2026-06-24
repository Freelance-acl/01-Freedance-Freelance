package com.team01.freelance.job.config;

import com.team01.freelance.job.security.JwtAuthFilter;
import com.team01.freelance.job.security.JwtResourceServerSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class JobSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return JwtResourceServerSecurityConfig.configure(http, jwtAuthFilter, "/api/jobs/health");
    }
}
