package com.project.sls.dto.auth;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn
) {
}
