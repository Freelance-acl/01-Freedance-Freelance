package com.team01.freelance.contract.security;

import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RoleAuthorizationHandler extends AbstractAuthHandler {

    @Override
    public boolean handle(AuthContext context) throws IOException {
        if (context.getRole() == null || !context.getRequiredRoles().contains(context.getRole())) {
            context.forbidden("Insufficient role");
            return false;
        }
        return delegate(context);
    }
}
