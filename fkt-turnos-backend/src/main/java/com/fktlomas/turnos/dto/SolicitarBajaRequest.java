package com.fktlomas.turnos.dto;

import jakarta.validation.constraints.Size;

public record SolicitarBajaRequest(
        @Size(max = 2000, message = "El motivo es demasiado largo") String motivo
) {
}
