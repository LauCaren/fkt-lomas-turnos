package com.fktlomas.turnos.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TendenciaDiaDto(LocalDate fecha, int turnosReservados, BigDecimal ingresos) {
}
