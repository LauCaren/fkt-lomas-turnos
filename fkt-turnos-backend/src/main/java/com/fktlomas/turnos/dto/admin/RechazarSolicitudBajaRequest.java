package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarSolicitudBajaRequest(
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 2000) String motivo
) {
}
