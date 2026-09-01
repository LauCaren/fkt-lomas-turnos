package com.fktlomas.turnos.dto.kinesiologia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RechazarPlanRequest(@NotBlank @Size(max = 500) String motivo) {
}
