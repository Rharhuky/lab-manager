package com.campuslab.laboratory;

import com.campuslab.laboratory.dto.LaboratoryResponse;
import com.campuslab.reservation.ReservationRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Property-based tests for LaboratoryService using jqwik.
 *
 * Feature: campus-lab, Property 4: Campo `reserved` reflete corretamente as FutureReservations
 * Validates: Requirements 3.4
 */
class LaboratoryServicePropertyTest {

    // --- Property 4: Campo `reserved` reflete corretamente as FutureReservations ---

    /**
     * A lab with only past reservations (startAt in the past) must have reserved=false.
     */
    @Property(tries = 100)
    void labWithOnlyPastReservationsIsNotReserved(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block
    ) {
        // Arrange
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findAll()).thenReturn(List.of(lab));
        // No future reservations — existsFutureReservation returns false
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(false);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        // Act
        List<LaboratoryResponse> result = service.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reserved())
                .as("Lab with only past reservations should not be reserved")
                .isFalse();
    }

    /**
     * A lab with at least one future reservation (startAt strictly after now) must have reserved=true.
     */
    @Property(tries = 100)
    void labWithAtLeastOneFutureReservationIsReserved(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block
    ) {
        // Arrange
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findAll()).thenReturn(List.of(lab));
        // There is at least one future reservation
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(true);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        // Act
        List<LaboratoryResponse> result = service.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reserved())
                .as("Lab with at least one future reservation should be reserved")
                .isTrue();
    }

    /**
     * A lab with a mix of past and future reservations must have reserved=true
     * (because at least one future reservation exists).
     */
    @Property(tries = 100)
    void labWithMixedReservationsIsReservedDueToFutureOnes(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block
    ) {
        // Arrange
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findAll()).thenReturn(List.of(lab));
        // Mixed reservations — future ones exist, so existsFutureReservation is true
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(true);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        // Act
        List<LaboratoryResponse> result = service.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reserved())
                .as("Lab with mixed past and future reservations should be reserved")
                .isTrue();
    }

    /**
     * A lab with no reservations at all must have reserved=false.
     */
    @Property(tries = 100)
    void labWithNoReservationsIsNotReserved(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block
    ) {
        // Arrange
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findAll()).thenReturn(List.of(lab));
        // No reservations at all
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(false);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        // Act
        List<LaboratoryResponse> result = service.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reserved())
                .as("Lab with no reservations should not be reserved")
                .isFalse();
    }

    /**
     * The `reserved` field is always a strict reflection of existsFutureReservation —
     * true if and only if the repository reports a future reservation.
     * Validates the biconditional: reserved ↔ existsFutureReservation.
     */
    @Property(tries = 100)
    void reservedFieldIsExactlyWhatRepositoryReports(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block,
            @ForAll boolean hasFutureReservation
    ) {
        // Arrange
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findAll()).thenReturn(List.of(lab));
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(hasFutureReservation);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        // Act
        List<LaboratoryResponse> result = service.findAll();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).reserved())
                .as("reserved field must match existsFutureReservation result (was %s)", hasFutureReservation)
                .isEqualTo(hasFutureReservation);
    }

    // --- helpers ---

    private Laboratory buildLab(UUID id, String name, String block) {
        Laboratory lab = new Laboratory(name, block);
        lab.setId(id);
        lab.setCreatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        lab.setUpdatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        return lab;
    }
}
