package com.fktlomas.turnos.dto.caja;

import com.fktlomas.turnos.domain.Ingreso;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IngresoDto(
        Long id,
        Long medicoId,
        String medicoNombre,
        Long turnoId,
        BigDecimal monto,
        String metodoPago,
        LocalDate fecha,
        String registradoPorNombre,
        boolean anulado,
        String motivoAnulacion,
        Instant createdAt
) {
    public static IngresoDto de(Ingreso i) {
        return new IngresoDto(
                i.getId(),
                i.getMedico().getId(),
                i.getMedico().getNombreCompleto(),
                i.getTurno() != null ? i.getTurno().getId() : null,
                i.getMonto(),
                i.getMetodoPago().name(),
                i.getFecha(),
                i.getRegistradoPor().getNombreCompleto(),
                i.isAnulado(),
                i.getMotivoAnulacion(),
                i.getCreatedAt()
        );
    }
}
