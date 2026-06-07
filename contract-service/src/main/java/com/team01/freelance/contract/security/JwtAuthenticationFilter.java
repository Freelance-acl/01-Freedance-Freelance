package com.team01.freelance.contract.security;

import com.team01.freelance.user.model.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthHandler head;

    public JwtAuthenticationFilter(
            TokenExtractionHandler tokenExtractionHandler,
            SignatureValidationHandler signatureValidationHandler,
            UserLoaderHandler userLoaderHandler,
            RoleAuthorizationHandler roleAuthorizationHandler
    ) {
        tokenExtractionHandler.setNext(signatureValidationHandler);
        signatureValidationHandler.setNext(userLoaderHandler);
        userLoaderHandler.setNext(roleAuthorizationHandler);
        this.head = tokenExtractionHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/contracts/health") || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        AuthContext context = new AuthContext(request, response);
        context.setRequiredRoles(java.util.EnumSet.allOf(UserRole.class));

        if (!head.handle(context)) {
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                context.getUser().getEmail(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + context.getRole().name()))
        );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        filterChain.doFilter(request, response);
    }
}
