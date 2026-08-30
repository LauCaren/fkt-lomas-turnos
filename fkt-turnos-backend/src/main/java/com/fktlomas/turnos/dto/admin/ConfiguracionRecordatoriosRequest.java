package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ConfiguracionRecordatoriosRequest(
        @NotBlank String plantillaMensaje,
        @Min(1) @Max(168) int horasAnticipacion
) {
}
