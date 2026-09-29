package com.campuslab.laboratory.dto;

import com.campuslab.reservation.dto.ReservationResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LaboratoryDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void laboratoryRequest_withValidData_shouldPassValidation() {
        LaboratoryRequest request = new LaboratoryRequest("Lab A", "Block B");
        
        Set<ConstraintViolation<LaboratoryRequest>> violations = validator.validate(request);
        
        assertThat(violations).isEmpty();
    }

    @Test
    void laboratoryRequest_withBlankName_shouldFailValidation() {
        LaboratoryRequest request = new LaboratoryRequest("", "Block B");
        
        Set<ConstraintViolation<LaboratoryRequest>> violations = validator.validate(request);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Nome do laboratório é obrigatório");
    }

    @Test
    void laboratoryRequest_withBlankBlock_shouldFailValidation() {
        LaboratoryRequest request = new LaboratoryRequest("Lab A", "");
        
        Set<ConstraintViolation<LaboratoryRequest>> violations = validator.validate(request);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Bloco é obrigatório");
    }

    @Test
    void laboratoryRequest_withNameTooLong_shouldFailValidation() {
        String longName = "a".repeat(101);
        LaboratoryRequest request = new LaboratoryRequest(longName, "Block B");
        
        Set<ConstraintViolation<LaboratoryRequest>> violations = validator.validate(request);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Nome do laboratório não pode exceder 100 caracteres");
    }

    @Test
    void laboratoryRequest_withBlockTooLong_shouldFailValidation() {
        String longBlock = "a".repeat(21);
        LaboratoryRequest request = new LaboratoryRequest("Lab A", longBlock);
        
        Set<ConstraintViolation<LaboratoryRequest>> violations = validator.validate(request);
        
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .isEqualTo("Bloco não pode exceder 20 caracteres");
    }

    @Test
    void laboratoryResponse_shouldCreateCorrectly() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        
        LaboratoryResponse response = new LaboratoryResponse(
            id, "Lab A", "Block B", false, now, now
        );
        
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Lab A");
        assertThat(response.block()).isEqualTo("Block B");
        assertThat(response.reserved()).isFalse();
        assertThat(response.createdAt()).isEqualTo(now);
        assertThat(response.updatedAt()).isEqualTo(now);
    }

    @Test
    void laboratoryDetailResponse_shouldCreateCorrectly() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        List<ReservationResponse> reservations = List.of();
        
        LaboratoryDetailResponse response = new LaboratoryDetailResponse(
            id, "Lab A", "Block B", true, now, now, reservations
        );
        
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Lab A");
        assertThat(response.block()).isEqualTo("Block B");
        assertThat(response.reserved()).isTrue();
        assertThat(response.reservations()).isEmpty();
    }

    @Test
    void laboratoryDetailResponse_shouldCreateFromBase() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        LaboratoryResponse base = new LaboratoryResponse(
            id, "Lab A", "Block B", false, now, now
        );
        
        ReservationResponse reservation = new ReservationResponse(
            UUID.randomUUID(),
            id,
            UUID.randomUUID(),
            now,
            now.plusSeconds(3600),
            now
        );
        List<ReservationResponse> reservations = List.of(reservation);
        
        LaboratoryDetailResponse detail = new LaboratoryDetailResponse(base, reservations);
        
        assertThat(detail.id()).isEqualTo(base.id());
        assertThat(detail.name()).isEqualTo(base.name());
        assertThat(detail.block()).isEqualTo(base.block());
        assertThat(detail.reserved()).isEqualTo(base.reserved());
        assertThat(detail.reservations()).hasSize(1);
    }
}
