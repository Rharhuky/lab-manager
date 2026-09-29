package com.campuslab.laboratory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LaboratoryRequest(
    @NotBlank(message = "Nome do laboratório é obrigatório")
    @Size(max = 100, message = "Nome do laboratório não pode exceder 100 caracteres")
    String name,
    
    @NotBlank(message = "Bloco é obrigatório")
    @Size(max = 20, message = "Bloco não pode exceder 20 caracteres")
    String block
) {
}
