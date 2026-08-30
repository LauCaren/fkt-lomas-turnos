package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.CanalNotificacion;
import jakarta.validation.constraints.NotNull;

public record CanalPreferidoRequest(@NotNull CanalNotificacion canal) {
}
