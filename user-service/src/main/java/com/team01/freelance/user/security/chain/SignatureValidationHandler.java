package com.team01.freelance.user.security.chain;

import com.team01.freelance.user.service.JwtService;

/**
 * Validates JWT signature and expiry when a token was extracted.
 */
public class SignatureValidationHandler extends AuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doHandle(AuthContext context) {
        if (context.getToken() == null) {
            return;
        }
        if (!jwtService.isTokenValid(context.getToken())) {
            context.fail(401, "Invalid or expired token");
        }
    }
}
