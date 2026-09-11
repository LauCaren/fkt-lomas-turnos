package com.fktlomas.turnos.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Pantalla de arranque del admin: lo que necesita saber de un vistazo al entrar. Los últimos
 * cuatro campos alimentan el panel "Requiere atención" — cada uno ya se puede resolver de un
 * conteo sobre una tabla que ya existe, no son un concepto nuevo.
 */
public record AdminResumenDto(
        LocalDate fecha,
        int turnosReservadosHoy,
        int turnosDisponiblesHoy,
        int pacientesEnEspera,
        int planesKinesiologiaPendientes,
        BigDecimal ingresosHoy,
        BigDecimal cajaFisica,
        int turnosSinConfirmarHoy,
        int mensajesSinLeer,
        int postulacionesSinRevisar,
        int solicitudesBajaPendientes
) {
}
