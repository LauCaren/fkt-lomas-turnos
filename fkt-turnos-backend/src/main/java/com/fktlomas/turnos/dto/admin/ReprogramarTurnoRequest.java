package com.fktlomas.turnos.dto.admin;

import jakarta.validation.constraints.NotNull;

/** El turno origen viaja en la URL (/turnos/{origenId}/reprogramar); acá solo el destino elegido. */
public record ReprogramarTurnoRequest(
        @NotNull(message = "Elegí el horario nuevo")
        Long turnoDestinoId
) {
}
