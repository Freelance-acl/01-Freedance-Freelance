package com.team01.freelance.user.dto;

public record AuthResponse(String token, long expiresIn) {
}
