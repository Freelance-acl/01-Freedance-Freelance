package com.team01.freelance.contract.security;

import com.team01.freelance.user.model.UserRole;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SignatureValidationHandler extends AbstractAuthHandler {

    private final JwtService jwtService;

    public SignatureValidationHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean handle(AuthContext context) throws IOException {
        try {
            Claims claims = jwtService.parseToken(context.getToken());
            context.setClaims(claims);
            Object uid = claims.get("uid");
            if (!(uid instanceof Number number)) {
                context.unauthorized("Token is missing uid claim");
                return false;
            }

            context.setUserId(number.longValue());
            context.setRole(UserRole.fromString(String.valueOf(claims.get("role"))));
            return delegate(context);
        } catch (Exception e) {
            context.unauthorized("Invalid or expired token");
            return false;
        }
    }
}
