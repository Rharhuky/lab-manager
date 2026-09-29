package com.campuslab.shared.exception.exceptions;

/**
 * Thrown when a reservation overlaps with an existing active reservation in the same laboratory.
 * Maps to HTTP 409 with code RESERVATION_CONFLICT.
 */
public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(String message) {
        super(message);
    }
}
