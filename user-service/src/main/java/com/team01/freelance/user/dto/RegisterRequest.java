package com.team01.freelance.user.dto;

public record RegisterRequest(String name, String email, String password, String role, String phone) {
}
