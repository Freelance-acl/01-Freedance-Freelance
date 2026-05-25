package com.team01.freelance.user.support;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.service.JwtService;

public final class TestAuthHelper {

    private TestAuthHelper() {
    }

    public static String adminToken(JwtService jwtService, UserRepository userRepository) {
        User admin = UserTestFixtures.seedAdmin(userRepository);
        return jwtService.generateToken(admin);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
