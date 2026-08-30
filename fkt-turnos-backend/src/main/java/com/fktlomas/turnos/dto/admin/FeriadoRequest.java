package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FeriadoRequest(
        @NotNull LocalDate fecha,
        @NotBlank @Size(max = 150) String nombre
) {
}
