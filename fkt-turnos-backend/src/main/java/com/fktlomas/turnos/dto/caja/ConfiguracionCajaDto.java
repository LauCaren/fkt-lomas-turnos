package com.fktlomas.turnos.dto.caja;

import com.fktlomas.turnos.domain.ConfiguracionCaja;

public record ConfiguracionCajaDto(String emailReporteMensual) {
    public static ConfiguracionCajaDto de(ConfiguracionCaja c) {
        return new ConfiguracionCajaDto(c.getEmailReporteMensual());
    }
}
