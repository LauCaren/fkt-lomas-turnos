package com.fktlomas.turnos.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactoRequest(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Email @Size(max = 180) String email,
        @Size(max = 30) String telefono,
        @NotBlank @Size(max = 2000) String mensaje
) {
}
