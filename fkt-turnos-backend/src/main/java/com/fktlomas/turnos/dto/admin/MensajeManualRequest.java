package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.CanalNotificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MensajeManualRequest(
        @NotNull Long pacienteId,
        @NotNull CanalNotificacion canal,
        @NotBlank @Size(max = 1600) String mensaje
) {
}
