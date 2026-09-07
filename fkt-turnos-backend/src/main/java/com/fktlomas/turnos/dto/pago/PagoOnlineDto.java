package com.fktlomas.turnos.dto.pago;

import com.fktlomas.turnos.domain.PagoOnline;

import java.math.BigDecimal;

public record PagoOnlineDto(
        Long id,
        Long turnoId,
        BigDecimal monto,
        String estado,
        String initPoint
) {
    public static PagoOnlineDto de(PagoOnline p, String initPoint) {
        return new PagoOnlineDto(p.getId(), p.getTurno().getId(), p.getMonto(), p.getEstado().name(), initPoint);
    }

    public static PagoOnlineDto de(PagoOnline p) {
        return de(p, null);
    }
}
