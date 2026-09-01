package com.fktlomas.turnos.dto.caja;

import com.fktlomas.turnos.domain.Retiro;

import java.math.BigDecimal;
import java.time.Instant;

public record RetiroDto(
        Long id,
        Long medicoId,
        String medicoNombre,
        BigDecimal monto,
        String entregadoPorNombre,
        Instant fechaHora,
        boolean anulado,
        String motivoAnulacion
) {
    public static RetiroDto de(Retiro r) {
        return new RetiroDto(
                r.getId(),
                r.getMedico().getId(),
                r.getMedico().getNombreCompleto(),
                r.getMonto(),
                r.getEntregadoPor().getNombreCompleto(),
                r.getFechaHora(),
                r.isAnulado(),
                r.getMotivoAnulacion()
        );
    }
}
