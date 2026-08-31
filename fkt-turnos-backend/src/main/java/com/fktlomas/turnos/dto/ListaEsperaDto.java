package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.ListaEspera;

import java.time.LocalDate;

public record ListaEsperaDto(
        Long id,
        Long medicoId,
        String medicoNombre,
        LocalDate fecha,
        String estado
) {
    public static ListaEsperaDto de(ListaEspera le) {
        return new ListaEsperaDto(
                le.getId(),
                le.getMedico().getId(),
                le.getMedico().getNombreCompleto(),
                le.getFecha(),
                le.getEstado().name()
        );
    }
}
