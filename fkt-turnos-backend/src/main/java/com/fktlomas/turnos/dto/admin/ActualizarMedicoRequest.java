package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ActualizarMedicoRequest(
        @NotBlank @Size(max = 150) String nombreCompleto,
        @NotNull Long especialidadId,
        @Size(max = 50) String matricula,
        boolean activo,
        @Min(10) int duracionTurnoMinutos,
        @Min(0) @Max(70) Integer anosExperiencia,
        boolean cobraCopagoIoma,
        @DecimalMin(value = "0", message = "La tarifa no puede ser negativa") BigDecimal tarifaConsulta
) {
}
