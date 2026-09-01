package com.fktlomas.turnos.dto.caja;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCategoriaGastoRequest(@NotBlank @Size(max = 100) String nombre) {
}
