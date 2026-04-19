package com.project.sls.dto.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        Integer userId,
        String name,
        String email,
        String role
) {}