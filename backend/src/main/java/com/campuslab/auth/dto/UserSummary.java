package com.campuslab.auth.dto;

import com.campuslab.user.UserRole;

import java.util.UUID;

public record UserSummary(
        UUID id,
        String name,
        String email,
        UserRole role
) {
}
