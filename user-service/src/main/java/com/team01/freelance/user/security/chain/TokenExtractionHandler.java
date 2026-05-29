package com.team01.freelance.user.security.chain;

/**
 * Extracts Bearer token from Authorization header when present.
 */
public class TokenExtractionHandler extends AuthHandler {

    @Override
    protected void doHandle(AuthContext context) {
        String header = context.getRequest().getHeader("Authorization");
        if (header == null || header.isBlank()) {
            return;
        }
        if (!header.startsWith("Bearer ")) {
            return;
        }
        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            context.fail(401, "Empty Bearer token");
            return;
        }
        context.setToken(token);
    }
}
