package br.leetjourney.smartguard.application.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        UUID tenantId,
        String email,
        String fullName,
        String role
) {

    public static AuthResponse of(String token, long expiresIn,
                                  br.leetjourney.smartguard.domain.model.User user) {
        return new AuthResponse(
                token,
                "Bearer",
                expiresIn,
                user.getId(),
                user.getTenant().getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }
}