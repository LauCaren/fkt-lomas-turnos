package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearMedicoRequest(
        @NotBlank @Size(max = 150) String nombreCompleto,
        @NotNull Long especialidadId,
        @Size(max = 50) String matricula,
        @Min(10) int duracionTurnoMinutos,
        @Min(0) @Max(70) Integer anosExperiencia
) {
}
