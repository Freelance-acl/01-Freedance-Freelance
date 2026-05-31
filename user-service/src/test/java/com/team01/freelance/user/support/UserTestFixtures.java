package com.team01.freelance.user.support;

import com.team01.freelance.user.model.User;
import com.team01.freelance.user.model.UserRole;
import com.team01.freelance.user.model.UserStatus;
import com.team01.freelance.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class UserTestFixtures {

    public static final String SEED_PASSWORD = "securePassword123";
    public static final String SEED_ADMIN_EMAIL = "admin@freelance.com";
    /** BCrypt hash of {@link #SEED_PASSWORD} (matches seed.sql). */
    public static final String SEED_PASSWORD_HASH =
            "$2a$10$.F.hAy2H0GbGYDuhvLYeZeyQ5j8bgncylLVySmYHITqFld1Uedpvq";

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    private UserTestFixtures() {
    }

    public static User seedAdmin(UserRepository userRepository) {
        if (userRepository.findByEmail(SEED_ADMIN_EMAIL).isPresent()) {
            return userRepository.findByEmail(SEED_ADMIN_EMAIL).orElseThrow();
        }
        User admin = new User();
        admin.setName("Admin User");
        admin.setEmail(SEED_ADMIN_EMAIL);
        admin.setPassword(SEED_PASSWORD_HASH);
        admin.setPhone("+201012345603");
        admin.setRole(UserRole.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        return userRepository.save(admin);
    }

    public static User saveUser(
            UserRepository userRepository,
            String name,
            String email,
            String phone,
            UserRole role,
            String plainPassword) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(ENCODER.encode(plainPassword));
        user.setPhone(phone);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.save(user);
    }
}
