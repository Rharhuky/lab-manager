package com.campuslab.reservation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ReservationRequest(
        @NotNull(message = "Data e hora de início são obrigatórias")
        Instant startAt,

        @NotNull(message = "Data e hora de término são obrigatórias")
        Instant endAt
) {
}
