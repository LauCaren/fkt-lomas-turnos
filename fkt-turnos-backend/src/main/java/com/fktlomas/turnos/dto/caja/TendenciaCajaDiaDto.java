package com.fktlomas.turnos.dto.caja;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TendenciaCajaDiaDto(LocalDate fecha, BigDecimal ingresos, BigDecimal gastos, BigDecimal retiros) {
}
