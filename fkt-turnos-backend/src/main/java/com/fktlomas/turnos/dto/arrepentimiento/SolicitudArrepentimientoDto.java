package com.fktlomas.turnos.dto.arrepentimiento;

import com.fktlomas.turnos.domain.EstadoArrepentimiento;
import com.fktlomas.turnos.domain.SolicitudArrepentimiento;

import java.time.Instant;

public record SolicitudArrepentimientoDto(
        Long id,
        String codigo,
        String nombreCompleto,
        String dni,
        String email,
        String telefono,
        String detalleOperacion,
        EstadoArrepentimiento estado,
        String notaResolucion,
        String resueltaPorNombre,
        Instant resueltaAt,
        Instant createdAt
) {
    public static SolicitudArrepentimientoDto de(SolicitudArrepentimiento s) {
        return new SolicitudArrepentimientoDto(s.getId(), s.getCodigo(), s.getNombreCompleto(), s.getDni(),
                s.getEmail(), s.getTelefono(), s.getDetalleOperacion(), s.getEstado(), s.getNotaResolucion(),
                s.getResueltaPor() != null ? s.getResueltaPor().getNombreCompleto() : null,
                s.getResueltaAt(), s.getCreatedAt());
    }
}
