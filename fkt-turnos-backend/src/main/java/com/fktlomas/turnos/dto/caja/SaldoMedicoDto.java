package com.fktlomas.turnos.dto.caja;

import java.math.BigDecimal;

public record SaldoMedicoDto(Long medicoId, String medicoNombre, BigDecimal ingresosTotal, BigDecimal retirosTotal, BigDecimal saldo) {
}
