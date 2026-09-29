package com.campuslab.shared.exception.exceptions;

/**
 * Thrown when reservation startAt >= endAt or duration < 60 seconds.
 * Maps to HTTP 400 with code INVALID_RESERVATION_PERIOD.
 */
public class InvalidReservationPeriodException extends RuntimeException {

    public InvalidReservationPeriodException(String message) {
        super(message);
    }
}
