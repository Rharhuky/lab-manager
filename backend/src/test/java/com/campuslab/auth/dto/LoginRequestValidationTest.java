package com.campuslab.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validLoginRequest_shouldPassValidation() {
        LoginRequest request = new LoginRequest("user@example.com", "password123");
        
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        
        assertThat(violations).isEmpty();
    }

    @Test
    void emptyEmail_shouldFailValidation() {
        LoginRequest request = new LoginRequest("", "password123");
        
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("obrigatório"));
    }

    @Test
    void invalidEmailFormat_shouldFailValidation() {
        LoginRequest request = new LoginRequest("notanemail", "password123");
        
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("formato válido"));
    }

    @Test
    void emailExceedingMaxSize_shouldFailValidation() {
        String longEmail = "a".repeat(250) + "@test.com"; // More than 255 characters
        LoginRequest request = new LoginRequest(longEmail, "password123");
        
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("255"));
    }

    @Test
    void emptyPassword_shouldFailValidation() {
        LoginRequest request = new LoginRequest("user@example.com", "");
        
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("obrigatória"));
    }

    @Test
    void passwordExceedingMaxSize_shouldFailValidation() {
        String longPassword = "a".repeat(260); // More than 255 characters
        LoginRequest request = new LoginRequest("user@example.com", longPassword);
        
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("255"));
    }
}
