package com.fktlomas.turnos.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Pantalla de arranque del admin: lo que necesita saber de un vistazo al entrar. */
public record AdminResumenDto(
        LocalDate fecha,
        int turnosReservadosHoy,
        int turnosDisponiblesHoy,
        int pacientesEnEspera,
        int planesKinesiologiaPendientes,
        BigDecimal ingresosHoy,
        BigDecimal cajaFisica
) {
}
