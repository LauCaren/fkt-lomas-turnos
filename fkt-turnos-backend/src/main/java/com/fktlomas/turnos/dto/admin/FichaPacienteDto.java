package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.dto.PacienteBusquedaDto;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.kinesiologia.PlanKinesiologiaDto;

import java.util.List;

/**
 * Vista consolidada de un paciente para cuando la secretaria necesita ver de una todo lo que
 * pasó con él/ella: turnos, planes de kinesiología, y los cobros asociados a sus turnos.
 * A propósito NO incluye historia clínica (evoluciones) — eso es privado del médico.
 */
public record FichaPacienteDto(
        PacienteBusquedaDto paciente,
        List<TurnoDto> turnos,
        List<PlanKinesiologiaDto> planesKinesiologia,
        List<IngresoDto> ingresos,
        long ausenciasCount
) {
}
