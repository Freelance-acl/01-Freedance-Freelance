package com.team01.freelance.contract.config;

import com.team01.freelance.user.config.JwtAuthBeans;
import com.team01.freelance.user.config.JwtResourceServerSecurityConfig;
import com.team01.freelance.user.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@Import(JwtAuthBeans.class)
public class ContractSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return JwtResourceServerSecurityConfig.configure(http, jwtAuthFilter, "/api/contracts/health");
    }
}
