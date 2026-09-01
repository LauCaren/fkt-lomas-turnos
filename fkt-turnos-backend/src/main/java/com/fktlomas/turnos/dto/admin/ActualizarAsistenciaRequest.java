package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.EstadoAsistencia;
import jakarta.validation.constraints.NotNull;

public record ActualizarAsistenciaRequest(@NotNull EstadoAsistencia estado) {
}
