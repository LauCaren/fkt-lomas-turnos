package com.fktlomas.turnos.dto.resena;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record CrearResenaRequest(
        @Min(value = 1, message = "El puntaje mínimo es 1") @Max(value = 5, message = "El puntaje máximo es 5") int puntaje,
        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres") String comentario
) {
}
