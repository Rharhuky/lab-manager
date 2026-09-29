package com.campuslab.reservation.dto;

import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(
    UUID id,
    UUID laboratoryId,
    UUID userId,
    Instant startAt,
    Instant endAt,
    Instant createdAt
) {
}
