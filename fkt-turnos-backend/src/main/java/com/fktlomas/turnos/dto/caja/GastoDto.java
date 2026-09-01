package com.fktlomas.turnos.dto.caja;

import com.fktlomas.turnos.domain.Gasto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record GastoDto(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal monto,
        LocalDate fecha,
        String descripcion,
        boolean tieneComprobante,
        String registradoPorNombre,
        boolean anulado,
        String motivoAnulacion,
        Instant createdAt
) {
    public static GastoDto de(Gasto g) {
        return new GastoDto(
                g.getId(),
                g.getCategoria().getId(),
                g.getCategoria().getNombre(),
                g.getMonto(),
                g.getFecha(),
                g.getDescripcion(),
                g.getComprobantePath() != null,
                g.getRegistradoPor().getNombreCompleto(),
                g.isAnulado(),
                g.getMotivoAnulacion(),
                g.getCreatedAt()
        );
    }
}
