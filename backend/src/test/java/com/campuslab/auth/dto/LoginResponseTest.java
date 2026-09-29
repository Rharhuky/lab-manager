package com.campuslab.auth.dto;

import com.campuslab.user.UserRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoginResponseTest {

    @Test
    void loginResponse_shouldContainAllRequiredFields() {
        UUID userId = UUID.randomUUID();
        UserSummary userSummary = new UserSummary(
                userId,
                "Test User",
                "test@example.com",
                UserRole.ALUNO
        );
        
        LoginResponse response = new LoginResponse(
                "jwt-token-here",
                "Bearer",
                3600L,
                userSummary
        );
        
        assertThat(response.token()).isEqualTo("jwt-token-here");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.user()).isNotNull();
        assertThat(response.user().id()).isEqualTo(userId);
        assertThat(response.user().name()).isEqualTo("Test User");
        assertThat(response.user().email()).isEqualTo("test@example.com");
        assertThat(response.user().role()).isEqualTo(UserRole.ALUNO);
    }

    @Test
    void userSummary_shouldContainAllRequiredFields() {
        UUID userId = UUID.randomUUID();
        UserSummary userSummary = new UserSummary(
                userId,
                "Professor Name",
                "professor@example.com",
                UserRole.PROFESSOR
        );
        
        assertThat(userSummary.id()).isEqualTo(userId);
        assertThat(userSummary.name()).isEqualTo("Professor Name");
        assertThat(userSummary.email()).isEqualTo("professor@example.com");
        assertThat(userSummary.role()).isEqualTo(UserRole.PROFESSOR);
    }

    @Test
    void loginResponse_shouldSupportAlunoRole() {
        UserSummary alunoUser = new UserSummary(
                UUID.randomUUID(),
                "Student Name",
                "student@example.com",
                UserRole.ALUNO
        );
        
        LoginResponse response = new LoginResponse(
                "token",
                "Bearer",
                7200L,
                alunoUser
        );
        
        assertThat(response.user().role()).isEqualTo(UserRole.ALUNO);
    }

    @Test
    void loginResponse_shouldSupportProfessorRole() {
        UserSummary professorUser = new UserSummary(
                UUID.randomUUID(),
                "Professor Name",
                "prof@example.com",
                UserRole.PROFESSOR
        );
        
        LoginResponse response = new LoginResponse(
                "token",
                "Bearer",
                7200L,
                professorUser
        );
        
        assertThat(response.user().role()).isEqualTo(UserRole.PROFESSOR);
    }
}
