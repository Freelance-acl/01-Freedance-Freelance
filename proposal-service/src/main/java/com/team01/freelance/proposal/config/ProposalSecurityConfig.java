package com.team01.freelance.proposal.config;

import com.team01.freelance.proposal.config.security.JwtAuthFilter;
import com.team01.freelance.proposal.config.security.JwtResourceServerSecurityConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ProposalSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return JwtResourceServerSecurityConfig.configure(http, jwtAuthFilter, "/api/proposals/health");
    }
}
