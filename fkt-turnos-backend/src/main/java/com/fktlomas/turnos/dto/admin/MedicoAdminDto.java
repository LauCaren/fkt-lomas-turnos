package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.Medico;

import java.math.BigDecimal;

/** Variante de MedicoDto solo para el panel de admin: incluye el email de login del médico. */
public record MedicoAdminDto(
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
        String usuarioEmail,
        BigDecimal tarifaConsulta
) {
    public static MedicoAdminDto de(Medico m) {
        return new MedicoAdminDto(
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
                m.getUsuario() != null ? m.getUsuario().getEmail() : null,
                m.getTarifaConsulta()
        );
    }
}
