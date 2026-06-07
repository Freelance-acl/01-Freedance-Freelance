package com.team01.freelance.contract.security;

import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class TokenExtractionHandler extends AbstractAuthHandler {

    @Override
    public boolean handle(AuthContext context) throws IOException {
        String authorization = context.getRequest().getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            context.unauthorized("Missing or invalid Authorization header");
            return false;
        }

        context.setToken(authorization.substring(7));
        return delegate(context);
    }
}
