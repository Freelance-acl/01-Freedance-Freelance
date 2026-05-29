package com.team01.freelance.user.security.chain;

/**
 * Role checks for endpoint access are enforced by {@code @PreAuthorize} after this filter.
 * This handler succeeds when authentication was established; missing auth on protected
 * routes is handled by Spring Security's entry point (401).
 */
public class RoleAuthorizationHandler extends AuthHandler {

    @Override
    protected void doHandle(AuthContext context) {
        // Method-level @PreAuthorize handles 403 for insufficient roles.
    }
}
