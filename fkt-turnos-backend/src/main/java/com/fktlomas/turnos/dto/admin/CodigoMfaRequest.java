package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CodigoMfaRequest(
        @NotBlank @Pattern(regexp = "\\d{6}", message = "El código debe tener 6 dígitos") String codigo
) {
}
