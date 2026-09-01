package com.fktlomas.turnos.dto.auditoria;

/** Estado mínimo de un Turno en un momento dado, para poder registrar y (des)hacer cambios. */
public record TurnoSnapshot(String estado, Long pacienteId, String motivoConsulta, String estadoAsistencia) {
}
