package com.campuslab.shared.exception.exceptions;

/**
 * Thrown when a requested resource is not found in the database.
 * Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
