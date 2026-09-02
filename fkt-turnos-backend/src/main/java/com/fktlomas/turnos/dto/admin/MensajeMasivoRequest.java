package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MensajeMasivoRequest(
        @NotNull LocalDate desde,
        @NotNull LocalDate hasta,
        Long medicoId,
        @NotBlank @Size(max = 1600) String mensaje
) {
}
