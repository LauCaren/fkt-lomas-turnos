package com.fktlomas.turnos.dto.medico;

import jakarta.validation.constraints.NotBlank;

public record CrearEvolucionRequest(@NotBlank(message = "La evolución no puede estar vacía") String texto) {
}
