package com.fktlomas.turnos.dto.arrepentimiento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolverArrepentimientoRequest(
        @NotBlank(message = "Anotá cómo se resolvió (ej. turno cancelado y pago reintegrado)")
        @Size(max = 1000)
        String notaResolucion
) {
}
