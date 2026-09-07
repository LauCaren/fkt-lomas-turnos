package com.fktlomas.turnos.dto.resena;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarResenaRequest(
        @NotBlank(message = "El motivo es obligatorio") @Size(max = 300) String motivo
) {
}
