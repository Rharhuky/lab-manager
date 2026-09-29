package com.campuslab.shared.dto;

import java.time.Instant;

/**
 * Standard error response format for all API errors.
 * Ensures consistent error handling across the application.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path
) {
    public ErrorResponse {
        if (message != null && message.length() > 512) {
            throw new IllegalArgumentException("Message cannot exceed 512 characters");
        }
    }
}
