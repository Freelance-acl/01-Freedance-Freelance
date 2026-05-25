package com.team01.freelance.job.support;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.service.JwtService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class TestAuthHelper {

    public static final String SEED_PASSWORD_HASH =
            "$2a$10$.F.hAy2H0GbGYDuhvLYeZeyQ5j8bgncylLVySmYHITqFld1Uedpvq";

    private TestAuthHelper() {
    }

    public static String adminToken(JwtService jwtService, UserRepository userRepository) {
        User admin = userRepository.findByEmail("admin@freelance.com").orElseGet(() -> {
            User user = new User();
            user.setName("Admin User");
            user.setEmail("admin@freelance.com");
            user.setPassword(SEED_PASSWORD_HASH);
            user.setPhone("+201012345603");
            user.setRole(UserRole.ADMIN);
            user.setStatus(UserStatus.ACTIVE);
            return userRepository.save(user);
        });
        return jwtService.generateToken(admin);
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
