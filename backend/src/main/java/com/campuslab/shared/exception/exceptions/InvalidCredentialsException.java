package com.campuslab.shared.exception.exceptions;

/**
 * Thrown when login credentials are invalid (wrong email or password).
 * Deliberately avoids indicating which field is incorrect (security best practice).
 * Maps to HTTP 401.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciais inválidas");
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
