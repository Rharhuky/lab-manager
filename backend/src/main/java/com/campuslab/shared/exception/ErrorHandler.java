package com.campuslab.shared.exception;

import com.campuslab.shared.dto.ErrorResponse;
import com.campuslab.shared.exception.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Global exception handler. Returns all errors as ErrorResponse.
 * Never exposes stack traces, class names or internal query details.
 * Requirements: 8.1, 8.2, 8.3
 */
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", ex.getMessage(), req);
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ResponseEntity<ErrorResponse> handleReservationConflict(
            ReservationConflictException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "RESERVATION_CONFLICT", ex.getMessage(), req);
    }

    @ExceptionHandler(LaboratoryHasFutureReservationsException.class)
    public ResponseEntity<ErrorResponse> handleLabHasReservations(
            LaboratoryHasFutureReservationsException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "LABORATORY_HAS_FUTURE_RESERVATIONS", ex.getMessage(), req);
    }

    @ExceptionHandler(DuplicateNameException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateName(
            DuplicateNameException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_LABORATORY_NAME", ex.getMessage(), req);
    }

    @ExceptionHandler(InvalidReservationPeriodException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPeriod(
            InvalidReservationPeriodException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_RESERVATION_PERIOD", ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                message.isEmpty() ? "Dados de entrada inválidos" : message, req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "O corpo da requisição está ausente ou malformado", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "INSUFFICIENT_PERMISSIONS",
                "Você não tem permissão para acessar este recurso", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(
            AuthenticationException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Autenticação é necessária para acessar este recurso", req);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(
            DataAccessException ex, HttpServletRequest req) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE",
                "O serviço está temporariamente indisponível. Tente novamente em instantes.", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest req) {
        // Never expose internal details
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Ocorreu um erro interno inesperado. Tente novamente em instantes.", req);
    }

    // --- helper ---

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String code, String message, HttpServletRequest req) {
        String safeMessage = message != null && message.length() > 512
                ? message.substring(0, 512)
                : message;
        ErrorResponse body = new ErrorResponse(
                Instant.now(), status.value(), code, safeMessage, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
