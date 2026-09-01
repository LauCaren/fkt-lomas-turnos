package com.fktlomas.turnos.dto.caja;

import com.fktlomas.turnos.domain.MetodoPago;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CrearIngresoRequest(
        @NotNull Long medicoId,
        Long turnoId,
        @NotNull @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0") BigDecimal monto,
        @NotNull MetodoPago metodoPago,
        LocalDate fecha
) {
}
