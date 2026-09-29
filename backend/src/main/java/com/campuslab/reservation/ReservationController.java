package com.campuslab.reservation;

import com.campuslab.reservation.dto.ReservationRequest;
import com.campuslab.reservation.dto.ReservationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for reservation endpoints.
 * Requirements: 5.1–5.5, 6.1–6.7
 */
@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** GET /api/laboratories/{labId}/reservations — list reservations ordered by startAt (HTTP 200) */
    @GetMapping("/api/laboratories/{labId}/reservations")
    public ResponseEntity<List<ReservationResponse>> findByLaboratory(@PathVariable UUID labId) {
        return ResponseEntity.ok(reservationService.findByLaboratory(labId));
    }

    /** GET /api/reservations/{id} — single reservation (HTTP 200) */
    @GetMapping("/api/reservations/{id}")
    public ResponseEntity<ReservationResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(reservationService.findById(id));
    }

    /** POST /api/laboratories/{labId}/reservations — create (HTTP 201) */
    @PostMapping("/api/laboratories/{labId}/reservations")
    public ResponseEntity<ReservationResponse> create(
            @PathVariable UUID labId,
            @Valid @RequestBody ReservationRequest request) {
        ReservationResponse created = reservationService.create(labId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/reservations/{id} — update (HTTP 200) */
    @PutMapping("/api/reservations/{id}")
    public ResponseEntity<ReservationResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.update(id, request));
    }

    /** DELETE /api/reservations/{id} — delete (HTTP 204) */
    @DeleteMapping("/api/reservations/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        reservationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
