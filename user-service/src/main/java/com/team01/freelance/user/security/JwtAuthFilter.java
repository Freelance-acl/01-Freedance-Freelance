package com.team01.freelance.user.security;

import com.team01.freelance.user.security.chain.AuthContext;
import com.team01.freelance.user.security.chain.AuthHandler;
import com.team01.freelance.user.security.chain.AuthHandlerChainFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AuthHandlerChainFactory chainFactory;

    public JwtAuthFilter(AuthHandlerChainFactory chainFactory) {
        this.chainFactory = chainFactory;
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

        AuthContext context = new AuthContext(request, response);
        AuthHandler head = chainFactory.buildChain();
        head.handle(context);

        if (context.hasFailed()) {
            response.sendError(context.getFailureStatus(), context.getFailureMessage());
            return;
        }

        if (context.getAuthentication() != null) {
            SecurityContextHolder.getContext().setAuthentication(context.getAuthentication());
        }

        chain.doFilter(request, response);
    }
}
