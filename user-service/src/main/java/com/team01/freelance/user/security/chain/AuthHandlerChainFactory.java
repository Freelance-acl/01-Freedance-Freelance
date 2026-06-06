package com.team01.freelance.user.security.chain;

import com.team01.freelance.user.service.JwtService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class AuthHandlerChainFactory {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public AuthHandlerChainFactory(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    public AuthHandler buildChain() {
        AuthHandler head = new TokenExtractionHandler();
        head.setNext(new SignatureValidationHandler(jwtService))
                .setNext(new UserLoaderHandler(jwtService, userDetailsService))
                .setNext(new RoleAuthorizationHandler());
        return head;
    }
}
