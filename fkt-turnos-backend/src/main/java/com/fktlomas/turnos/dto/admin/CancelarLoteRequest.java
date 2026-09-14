package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CancelarLoteRequest(
        @NotEmpty(message = "Elegí al menos un turno")
        List<Long> turnoIds
) {
}
