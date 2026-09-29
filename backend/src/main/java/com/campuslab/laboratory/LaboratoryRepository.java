package com.campuslab.laboratory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LaboratoryRepository extends JpaRepository<Laboratory, UUID> {

    /**
     * Finds a laboratory by its name.
     *
     * @param name the laboratory name
     * @return an Optional containing the Laboratory if found, empty otherwise
     */
    Optional<Laboratory> findByName(String name);

    /**
     * Checks if a laboratory with the given name exists, excluding the specified id.
     * This is useful for validating unique names during updates.
     *
     * @param name the laboratory name to check
     * @param id   the laboratory id to exclude from the check
     * @return true if a laboratory with the given name exists (excluding the specified id), false otherwise
     */
    boolean existsByNameAndIdNot(String name, UUID id);
}
