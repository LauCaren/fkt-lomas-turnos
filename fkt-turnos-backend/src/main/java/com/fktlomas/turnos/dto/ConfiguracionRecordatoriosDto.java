package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.ConfiguracionRecordatorios;

public record ConfiguracionRecordatoriosDto(String plantillaMensaje, int horasAnticipacion) {
    public static ConfiguracionRecordatoriosDto de(ConfiguracionRecordatorios c) {
        return new ConfiguracionRecordatoriosDto(c.getPlantillaMensaje(), c.getHorasAnticipacion());
    }
}
