package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GenerarTurnosRequest(
        @NotNull LocalDate desde,
        @NotNull LocalDate hasta
) {
}
