package com.fktlomas.turnos.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ListaEsperaRequest(
        @NotNull Long medicoId,
        @NotNull LocalDate fecha
) {
}
