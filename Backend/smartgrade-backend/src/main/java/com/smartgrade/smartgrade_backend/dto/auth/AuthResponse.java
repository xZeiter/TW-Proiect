package com.smartgrade.smartgrade_backend.dto.auth;

public record AuthResponse(
        Long id,
        String email,
        String token
) {}
