package com.campuslab.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, UUID> {

    /**
     * Checks for overlapping active reservations in the same laboratory.
     * Excludes a specific reservation by ID (for update scenarios, pass the existing reservation's ID;
     * for create scenarios, pass a nil UUID that won't match any real reservation).
     *
     * Two reservations overlap when NOT (A.endAt <= B.startAt OR B.endAt <= A.startAt).
     * Only considers reservations whose endAt is in the future (active reservations).
     *
     * Requirements: 3.4, 4.4, 5.1, 7.2, 7.4
     */
    @Query("""
        SELECT COUNT(r) > 0
        FROM Reservation r
        WHERE r.laboratory.id = :labId
          AND r.id <> :excludeId
          AND r.endAt > :now
          AND r.startAt < :endAt
          AND r.endAt > :startAt
        """)
    boolean existsConflict(
        @Param("labId") UUID labId,
        @Param("excludeId") UUID excludeId,
        @Param("startAt") Instant startAt,
        @Param("endAt") Instant endAt,
        @Param("now") Instant now
    );

    /**
     * Checks if there is any future reservation for a laboratory.
     * Used to determine the 'reserved' status and prevent deletion of labs with future bookings.
     *
     * Requirements: 3.4, 4.4
     */
    @Query("""
        SELECT COUNT(r) > 0
        FROM Reservation r
        WHERE r.laboratory.id = :labId
          AND r.startAt > :now
        """)
    boolean existsFutureReservation(
        @Param("labId") UUID labId,
        @Param("now") Instant now
    );

    /**
     * Returns all reservations for a laboratory ordered by startAt ascending.
     *
     * Requirements: 5.1
     */
    List<Reservation> findByLaboratoryIdOrderByStartAtAsc(UUID laboratoryId);
}
