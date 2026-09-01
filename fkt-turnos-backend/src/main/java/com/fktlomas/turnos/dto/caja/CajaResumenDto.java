package com.fktlomas.turnos.dto.caja;

import java.math.BigDecimal;
import java.util.List;

/**
 * "Caja física" (lo que efectivamente queda en el consultorio) e "importe a favor de cada
 * médico" son dos cosas separadas: la primera descuenta los gastos operativos del centro,
 * la segunda es pura y exclusivamente lo cobrado a nombre de ese médico menos lo que ya retiró.
 */
public record CajaResumenDto(
        BigDecimal ingresosTotal,
        BigDecimal retirosTotal,
        BigDecimal gastosTotal,
        BigDecimal cajaFisica,
        List<SaldoMedicoDto> saldosPorMedico
) {
}
