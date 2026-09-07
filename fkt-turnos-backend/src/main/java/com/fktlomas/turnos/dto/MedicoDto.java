package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Medico;

import java.math.BigDecimal;

public record MedicoDto(
        Long id,
        String nombreCompleto,
        Long especialidadId,
        String especialidadNombre,
        String matricula,
        boolean activo,
        int duracionTurnoMinutos,
        Integer anosExperiencia,
        boolean tieneFoto,
        boolean tieneTitulo,
        boolean cobraCopagoIoma,
        BigDecimal tarifaConsulta
) {
    public static MedicoDto de(Medico m) {
        return new MedicoDto(
                m.getId(),
                m.getNombreCompleto(),
                m.getEspecialidad().getId(),
                m.getEspecialidad().getNombre(),
                m.getMatricula(),
                m.isActivo(),
                m.getDuracionTurnoMinutos(),
                m.getAnosExperiencia(),
                m.getFotoPath() != null,
                m.getTituloImagenPath() != null,
                m.isCobraCopagoIoma(),
                m.getTarifaConsulta()
        );
    }
}
