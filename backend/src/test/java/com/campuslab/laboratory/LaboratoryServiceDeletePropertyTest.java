package com.campuslab.laboratory;

import com.campuslab.reservation.ReservationRepository;
import com.campuslab.shared.exception.exceptions.LaboratoryHasFutureReservationsException;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.NotBlank;
import net.jqwik.api.constraints.Size;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for LaboratoryService#delete using jqwik.
 *
 * Feature: campus-lab, Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada
 * Validates: Requirements 4.4
 */
class LaboratoryServiceDeletePropertyTest {

    // --- Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada ---

    /**
     * For any laboratory with exactly 1 future reservation, delete must always throw
     * LaboratoryHasFutureReservationsException — regardless of lab name/block.
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada
    void labWithOneFutureReservationIsAlwaysRejected(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block
    ) {
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findById(labId)).thenReturn(Optional.of(lab));
        // Exactly 1 future reservation exists
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(true);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        assertThatThrownBy(() -> service.delete(labId))
                .as("DELETE must be rejected with LaboratoryHasFutureReservationsException " +
                    "when exactly 1 future reservation exists (lab=%s, block=%s)", labName, block)
                .isInstanceOf(LaboratoryHasFutureReservationsException.class);

        verify(labRepo, never()).delete(any(Laboratory.class));
    }

    /**
     * For any laboratory with N future reservations and M past reservations (N >= 1, M >= 0),
     * delete must always be rejected. The presence of past reservations is irrelevant.
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada
    void labWithFutureAndPastReservationsIsAlwaysRejected(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block,
            @ForAll @IntRange(min = 1, max = 50) int futureCount,
            @ForAll @IntRange(min = 0, max = 50) int pastCount
    ) {
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findById(labId)).thenReturn(Optional.of(lab));
        // futureCount >= 1 means existsFutureReservation is true regardless of pastCount
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(true);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        assertThatThrownBy(() -> service.delete(labId))
                .as("DELETE must be rejected when %d future + %d past reservations exist " +
                    "(lab=%s, block=%s)", futureCount, pastCount, labName, block)
                .isInstanceOf(LaboratoryHasFutureReservationsException.class);

        verify(labRepo, never()).delete(any(Laboratory.class));
    }

    /**
     * For any laboratory with only past reservations (no future ones), delete must succeed
     * without throwing any exception.
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada
    void labWithOnlyPastReservationsIsAlwaysAllowed(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block
    ) {
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findById(labId)).thenReturn(Optional.of(lab));
        // No future reservations — all are in the past
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(false);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        assertThatCode(() -> service.delete(labId))
                .as("DELETE must succeed when lab has only past reservations (lab=%s, block=%s)",
                    labName, block)
                .doesNotThrowAnyException();

        verify(labRepo).delete(lab);
    }

    /**
     * For any laboratory with zero reservations, delete must succeed without throwing
     * any exception.
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada
    void labWithNoReservationsIsAlwaysAllowed(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block
    ) {
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findById(labId)).thenReturn(Optional.of(lab));
        // No reservations at all
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class))).thenReturn(false);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        assertThatCode(() -> service.delete(labId))
                .as("DELETE must succeed when lab has no reservations (lab=%s, block=%s)",
                    labName, block)
                .doesNotThrowAnyException();

        verify(labRepo).delete(lab);
    }

    /**
     * The rejection decision is purely binary: any truthy result from existsFutureReservation
     * always causes rejection. Tested across arbitrary lab names/blocks.
     */
    @Property(tries = 100)
    // Feature: campus-lab, Property 5: Exclusão de laboratório com reservas futuras sempre é rejeitada
    void rejectionIsExactlyDrivenByExistsFutureReservation(
            @ForAll @NotBlank @Size(max = 100) String labName,
            @ForAll @NotBlank @Size(max = 20) String block,
            @ForAll boolean hasFutureReservation
    ) {
        UUID labId = UUID.randomUUID();
        Laboratory lab = buildLab(labId, labName, block);

        LaboratoryRepository labRepo = mock(LaboratoryRepository.class);
        ReservationRepository resRepo = mock(ReservationRepository.class);

        when(labRepo.findById(labId)).thenReturn(Optional.of(lab));
        when(resRepo.existsFutureReservation(eq(labId), any(Instant.class)))
                .thenReturn(hasFutureReservation);

        LaboratoryService service = new LaboratoryService(labRepo, resRepo);

        if (hasFutureReservation) {
            assertThatThrownBy(() -> service.delete(labId))
                    .as("DELETE must throw when existsFutureReservation=true (lab=%s)", labName)
                    .isInstanceOf(LaboratoryHasFutureReservationsException.class);
            verify(labRepo, never()).delete(any(Laboratory.class));
        } else {
            assertThatCode(() -> service.delete(labId))
                    .as("DELETE must succeed when existsFutureReservation=false (lab=%s)", labName)
                    .doesNotThrowAnyException();
            verify(labRepo).delete(lab);
        }
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
