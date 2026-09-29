package com.campuslab.laboratory;

import com.campuslab.auth.AuthService;
import com.campuslab.auth.dto.LoginRequest;
import com.campuslab.laboratory.dto.LaboratoryRequest;
import com.campuslab.user.User;
import com.campuslab.user.UserRepository;
import com.campuslab.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for LaboratoryController.
 * Covers access control (ALUNO vs PROFESSOR) and CRUD scenarios.
 * Requirements: 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class LaboratoryControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired LaboratoryRepository laboratoryRepository;
    @Autowired BCryptPasswordEncoder passwordEncoder;
    @Autowired AuthService authService;

    private String professorToken;
    private String alunoToken;

    @BeforeEach
    void setUp() {
        laboratoryRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(new User("Professor", "prof@test.com",
                passwordEncoder.encode("senha123"), UserRole.PROFESSOR));
        userRepository.save(new User("Aluno", "aluno@test.com",
                passwordEncoder.encode("senha123"), UserRole.ALUNO));

        professorToken = authService.login(new LoginRequest("prof@test.com", "senha123")).token();
        alunoToken = authService.login(new LoginRequest("aluno@test.com", "senha123")).token();
    }

    // --- GET /api/laboratories ---

    @Test
    void getAll_asProfessor_returns200() throws Exception {
        mockMvc.perform(get("/api/laboratories")
                .header("Authorization", "Bearer " + professorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getAll_asAluno_returns200() throws Exception {
        mockMvc.perform(get("/api/laboratories")
                .header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/laboratories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // --- POST /api/laboratories ---

    @Test
    void create_asProfessor_returns201() throws Exception {
        LaboratoryRequest req = new LaboratoryRequest("Lab Novo", "Bloco A");
        mockMvc.perform(post("/api/laboratories")
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.name").value("Lab Novo"))
                .andExpect(jsonPath("$.block").value("Bloco A"))
                .andExpect(jsonPath("$.reserved").value(false));
    }

    @Test
    void create_asAluno_returns403() throws Exception {
        LaboratoryRequest req = new LaboratoryRequest("Lab Aluno", "Bloco B");
        mockMvc.perform(post("/api/laboratories")
                .header("Authorization", "Bearer " + alunoToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_PERMISSIONS"));
    }

    @Test
    void create_withBlankName_returns400() throws Exception {
        LaboratoryRequest req = new LaboratoryRequest("", "Bloco A");
        mockMvc.perform(post("/api/laboratories")
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void create_withDuplicateName_returns409() throws Exception {
        laboratoryRepository.save(new Laboratory("Lab Dup", "Bloco X"));

        LaboratoryRequest req = new LaboratoryRequest("Lab Dup", "Bloco Y");
        mockMvc.perform(post("/api/laboratories")
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_LABORATORY_NAME"));
    }

    // --- GET /api/laboratories/{id} ---

    @Test
    void getById_existingId_returns200WithReservations() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory("Lab Detail", "Bloco D"));

        mockMvc.perform(get("/api/laboratories/{id}", lab.getId())
                .header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(lab.getId().toString()))
                .andExpect(jsonPath("$.reservations").isArray());
    }

    @Test
    void getById_nonExistentId_returns404() throws Exception {
        mockMvc.perform(get("/api/laboratories/{id}", java.util.UUID.randomUUID())
                .header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    // --- PUT /api/laboratories/{id} ---

    @Test
    void update_asProfessor_returns200() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory("Lab Old", "Bloco E"));
        LaboratoryRequest req = new LaboratoryRequest("Lab Updated", "Bloco F");

        mockMvc.perform(put("/api/laboratories/{id}", lab.getId())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lab Updated"));
    }

    @Test
    void update_nonExistentId_returns404() throws Exception {
        LaboratoryRequest req = new LaboratoryRequest("Lab X", "Bloco X");
        mockMvc.perform(put("/api/laboratories/{id}", java.util.UUID.randomUUID())
                .header("Authorization", "Bearer " + professorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /api/laboratories/{id} ---

    @Test
    void delete_asProfessor_noFutureReservations_returns204() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory("Lab Del", "Bloco G"));

        mockMvc.perform(delete("/api/laboratories/{id}", lab.getId())
                .header("Authorization", "Bearer " + professorToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_nonExistentId_returns404() throws Exception {
        mockMvc.perform(delete("/api/laboratories/{id}", java.util.UUID.randomUUID())
                .header("Authorization", "Bearer " + professorToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_asAluno_returns403() throws Exception {
        Laboratory lab = laboratoryRepository.save(new Laboratory("Lab Aluno Del", "Bloco H"));

        mockMvc.perform(delete("/api/laboratories/{id}", lab.getId())
                .header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isForbidden());
    }
}
