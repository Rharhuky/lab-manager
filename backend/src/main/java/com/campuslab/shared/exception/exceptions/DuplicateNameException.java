package com.campuslab.shared.exception.exceptions;

/**
 * Thrown when attempting to create/update a laboratory with a name that already exists.
 * Maps to HTTP 409.
 */
public class DuplicateNameException extends RuntimeException {

    public DuplicateNameException(String message) {
        super(message);
    }
}
