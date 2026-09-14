package com.fktlomas.turnos.dto.admin;

import java.util.List;

/**
 * Resultado de cancelar varios turnos a la vez (AdminAgendaTab.jsx, ej. un médico avisa que no
 * viene un día entero) — cada turno se cancela en su propia transacción (ver
 * AdminTurnoController.cancelarLote), así que uno que ya no esté RESERVADO no aborta el resto:
 * simplemente queda en `errores` con el motivo, mientras los demás se procesan igual.
 */
public record CancelarLoteResponse(
        List<Long> cancelados,
        List<ErrorCancelacion> errores
) {
    public record ErrorCancelacion(Long turnoId, String mensaje) {
    }
}
