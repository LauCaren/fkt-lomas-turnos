package com.fktlomas.turnos.dto.caja;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularRequest(@NotBlank @Size(max = 300) String motivo) {
}
