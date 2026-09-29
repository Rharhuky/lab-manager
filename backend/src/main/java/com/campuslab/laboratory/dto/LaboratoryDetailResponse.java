package com.campuslab.laboratory.dto;

import com.campuslab.reservation.dto.ReservationResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LaboratoryDetailResponse(
    UUID id,
    String name,
    String block,
    Boolean reserved,
    Instant createdAt,
    Instant updatedAt,
    List<ReservationResponse> reservations
) {
    // Constructor that extends LaboratoryResponse
    public LaboratoryDetailResponse(LaboratoryResponse base, List<ReservationResponse> reservations) {
        this(
            base.id(),
            base.name(),
            base.block(),
            base.reserved(),
            base.createdAt(),
            base.updatedAt(),
            reservations
        );
    }
}
