package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.dto.TurnoAdminDto;

/** Los dos turnos que cambiaron — origen (quedó libre) y destino (quedó reservado) — para que el frontend actualice ambas celdas de la agenda sin recargar todo. */
public record ReprogramarTurnoResponse(TurnoAdminDto origen, TurnoAdminDto destino) {
}
