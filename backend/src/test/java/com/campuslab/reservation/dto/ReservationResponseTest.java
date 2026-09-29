package com.campuslab.reservation.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationResponseTest {

    @Test
    void reservationResponse_shouldCreateCorrectly() {
        UUID id = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant later = now.plusSeconds(3600);
        
        ReservationResponse response = new ReservationResponse(
            id, labId, userId, now, later, now
        );
        
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.laboratoryId()).isEqualTo(labId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.startAt()).isEqualTo(now);
        assertThat(response.endAt()).isEqualTo(later);
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void reservationResponse_shouldBeImmutable() {
        UUID id = UUID.randomUUID();
        UUID labId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant later = now.plusSeconds(3600);
        
        ReservationResponse response1 = new ReservationResponse(
            id, labId, userId, now, later, now
        );
        ReservationResponse response2 = new ReservationResponse(
            id, labId, userId, now, later, now
        );
        
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }
}
