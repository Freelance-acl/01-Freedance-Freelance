package com.team01.freelance.user.service;

import com.team01.freelance.common.observer.EventSubject;
import com.team01.freelance.user.dto.AuthResponse;
import com.team01.freelance.user.dto.LoginRequest;
import com.team01.freelance.user.dto.RegisterRequest;
import com.team01.freelance.user.model.User;
import com.team01.freelance.user.messaging.UserEventPublisher;
import com.team01.freelance.user.repository.UserRepository;
import com.team01.freelance.user.model.UserRole;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AuthService {

    @Autowired(required = false)
    private UserEventPublisher userEventPublisher;

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EventSubject authEventSubject;

    public AuthService(
            UserRepository userRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            EventSubject authEventSubject) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authEventSubject = authEventSubject;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration failed: email already exists - {}", request.email());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        if (request.role() != null && !"ADMIN".equalsIgnoreCase(request.role())) {
            user.setRole(UserRole.fromString(request.role()));
        } else {
            user.setRole(UserRole.CLIENT);
        }
        user.setPhone(request.phone());
        user = userRepository.save(user);

        if (userEventPublisher != null) {
            userEventPublisher.publishUserRegistered(user);
        }

        authEventSubject.notifyObservers("REGISTERED", Map.of(
                "userId", user.getId(),
                "action", "REGISTERED",
                "details", Map.of("email", user.getEmail())));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtServiceExpirationMs());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        authEventSubject.notifyObservers("LOGGED_IN", Map.of(
                "userId", user.getId(),
                "action", "LOGGED_IN",
                "details", Map.of()));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtServiceExpirationMs());
    }

    private static long jwtServiceExpirationMs() {
        return com.team01.freelance.user.config.JwtConfigurationManager.getInstance().getExpiration();
    }
}
