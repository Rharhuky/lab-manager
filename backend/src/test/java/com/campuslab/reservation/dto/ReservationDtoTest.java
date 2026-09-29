package com.campuslab.reservation.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void reservationRequest_withValidData_shouldPassValidation() {
        // Given
        Instant start = Instant.parse("2025-06-01T14:00:00Z");
        Instant end = Instant.parse("2025-06-01T16:00:00Z");
        ReservationRequest request = new ReservationRequest(start, end);

        // When
        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    void reservationRequest_withNullStartAt_shouldFailValidation() {
        // Given
        ReservationRequest request = new ReservationRequest(null, Instant.now());

        // When
        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Data e hora de início são obrigatórias");
    }

    @Test
    void reservationRequest_withNullEndAt_shouldFailValidation() {
        // Given
        ReservationRequest request = new ReservationRequest(Instant.now(), null);

        // When
        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Data e hora de término são obrigatórias");
    }

    @Test
    void reservationRequest_withBothNull_shouldFailValidationWithTwoErrors() {
        // Given
        ReservationRequest request = new ReservationRequest(null, null);

        // When
        Set<ConstraintViolation<ReservationRequest>> violations = validator.validate(request);

        // Then
        assertThat(violations).hasSize(2);
    }

    @Test
    void reservationResponse_shouldCreateInstanceWithAllFields() {
        // Given
        UUID id = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant start = Instant.parse("2025-06-01T14:00:00Z");
        Instant end = Instant.parse("2025-06-01T16:00:00Z");
        Instant created = Instant.now();

        // When
        ReservationResponse response = new ReservationResponse(id, labId, userId, start, end, created);

        // Then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.laboratoryId()).isEqualTo(labId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.startAt()).isEqualTo(start);
        assertThat(response.endAt()).isEqualTo(end);
        assertThat(response.createdAt()).isEqualTo(created);
    }
}
