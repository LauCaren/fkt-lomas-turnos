package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Especialidad;

public record EspecialidadDto(Long id, String nombre, boolean esKinesiologia) {
    public static EspecialidadDto de(Especialidad e) {
        return new EspecialidadDto(e.getId(), e.getNombre(), e.isEsKinesiologia());
    }
}
