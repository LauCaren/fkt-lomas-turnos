package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.dto.kinesiologia.PlanKinesiologiaDto;

import java.time.Instant;
import java.util.List;

/** Derecho de acceso a los propios datos personales (Ley 25.326) — todo lo que el sistema tiene sobre el usuario, en un solo bundle descargable. */
public record ExportarDatosDto(
        Instant generadoEl,
        PerfilDto perfil,
        List<TurnoDto> turnos,
        List<PlanKinesiologiaDto> planesKinesiologia,
        List<EvolucionPacienteDto> historiaClinica
) {
}
