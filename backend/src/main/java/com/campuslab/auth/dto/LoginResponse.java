package com.campuslab.auth.dto;

public record LoginResponse(
        String token,
        String type,
        long expiresIn,
        UserSummary user
) {
}
