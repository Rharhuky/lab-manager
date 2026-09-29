package com.campuslab.laboratory;

import com.campuslab.laboratory.dto.LaboratoryDetailResponse;
import com.campuslab.laboratory.dto.LaboratoryRequest;
import com.campuslab.laboratory.dto.LaboratoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for laboratory CRUD endpoints.
 * Requirements: 3.1, 3.2, 3.3, 3.5, 4.1, 4.2, 4.3, 4.5, 4.6, 4.7
 */
@RestController
@RequestMapping("/api/laboratories")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    public LaboratoryController(LaboratoryService laboratoryService) {
        this.laboratoryService = laboratoryService;
    }

    /** GET /api/laboratories — list all (HTTP 200) */
    @GetMapping
    public ResponseEntity<List<LaboratoryResponse>> findAll() {
        return ResponseEntity.ok(laboratoryService.findAll());
    }

    /** GET /api/laboratories/{id} — detail with reservations (HTTP 200) */
    @GetMapping("/{id}")
    public ResponseEntity<LaboratoryDetailResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(laboratoryService.findById(id));
    }

    /** POST /api/laboratories — create (HTTP 201) */
    @PostMapping
    public ResponseEntity<LaboratoryResponse> create(@Valid @RequestBody LaboratoryRequest request) {
        LaboratoryResponse created = laboratoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/laboratories/{id} — update (HTTP 200) */
    @PutMapping("/{id}")
    public ResponseEntity<LaboratoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody LaboratoryRequest request) {
        return ResponseEntity.ok(laboratoryService.update(id, request));
    }

    /** DELETE /api/laboratories/{id} — delete (HTTP 204) */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        laboratoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
