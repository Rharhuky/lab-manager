package com.campuslab.shared.exception.exceptions;

/**
 * Thrown when attempting to delete a laboratory that has future reservations.
 * Maps to HTTP 409 with code LABORATORY_HAS_FUTURE_RESERVATIONS.
 */
public class LaboratoryHasFutureReservationsException extends RuntimeException {

    public LaboratoryHasFutureReservationsException(String message) {
        super(message);
    }
}
