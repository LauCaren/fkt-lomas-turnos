package com.fktlomas.turnos.dto.caja;

import jakarta.validation.constraints.Email;

/** emailReporteMensual en null/blank desactiva el envío automático mensual. */
public record ActualizarConfiguracionCajaRequest(
        @Email(message = "El email no es válido") String emailReporteMensual
) {
}
