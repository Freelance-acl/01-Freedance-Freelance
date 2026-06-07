package com.team01.freelance.contract.security;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class UserLoaderHandler extends AbstractAuthHandler {

    private final UserRepository userRepository;

    public UserLoaderHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean handle(AuthContext context) throws IOException {
        User user = userRepository.findById(context.getUserId()).orElse(null);
        if (user == null) {
            context.unauthorized("Authenticated user was not found");
            return false;
        }

        context.setUser(user);
        return delegate(context);
    }
}
