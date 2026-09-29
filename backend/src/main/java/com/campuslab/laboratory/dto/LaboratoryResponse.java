package com.campuslab.laboratory.dto;

import java.time.Instant;
import java.util.UUID;

public record LaboratoryResponse(
    UUID id,
    String name,
    String block,
    Boolean reserved,
    Instant createdAt,
    Instant updatedAt
) {
}
