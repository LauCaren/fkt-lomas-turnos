package com.fktlomas.turnos.dto.auditoria;

import com.fktlomas.turnos.domain.RegistroAuditoria;

import java.time.Instant;

public record RegistroAuditoriaDto(
        Long id,
        String usuarioNombre,
        String tipoEntidad,
        Long entidadId,
        String operacion,
        String snapshotAntes,
        String snapshotDespues,
        boolean deshecho,
        Instant createdAt
) {
    public static RegistroAuditoriaDto de(RegistroAuditoria r) {
        return new RegistroAuditoriaDto(
                r.getId(),
                r.getUsuario() != null ? r.getUsuario().getNombreCompleto() : "Sistema",
                r.getTipoEntidad(),
                r.getEntidadId(),
                r.getOperacion(),
                r.getSnapshotAntes(),
                r.getSnapshotDespues(),
                r.isDeshecho(),
                r.getCreatedAt()
        );
    }
}
