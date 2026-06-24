package com.team01.freelance.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PublicEndpoints.isPublic(
                HttpMethod.valueOf(request.getMethod()),
                resolvePath(request));
    }

    private static String resolvePath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path != null && !path.isEmpty()) {
            return path;
        }
        path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            return path.substring(contextPath.length());
        }
        return path;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (token.isEmpty()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Empty Bearer token");
                return;
            }
            if (!jwtService.isTokenValid(token)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
                return;
            }
            String username = jwtService.extractUsername(token);
            String role = jwtService.extractRole(token);
            if (username == null || username.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token missing subject");
                return;
            }
            String authority = role == null || role.isBlank()
                    ? "ROLE_USER"
                    : (role.startsWith("ROLE_") ? role : "ROLE_" + role);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    List.of(new SimpleGrantedAuthority(authority)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        chain.doFilter(request, response);
    }
}
