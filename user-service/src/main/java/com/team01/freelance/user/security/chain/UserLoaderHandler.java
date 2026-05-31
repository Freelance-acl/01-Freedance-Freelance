package com.team01.freelance.user.security.chain;

import com.team01.freelance.user.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Loads {@link UserDetails} from PostgreSQL after a valid token.
 */
public class UserLoaderHandler extends AuthHandler {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public UserLoaderHandler(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doHandle(AuthContext context) {
        if (context.getToken() == null) {
            return;
        }
        String username = jwtService.extractUsername(context.getToken());
        if (username == null || username.isBlank()) {
            context.fail(401, "Token missing subject");
            return;
        }
        try {
            UserDetails details = userDetailsService.loadUserByUsername(username);
            context.setAuthentication(new UsernamePasswordAuthenticationToken(
                    details, null, details.getAuthorities()));
        } catch (Exception ex) {
            context.fail(401, "User not found for token");
        }
    }
}
