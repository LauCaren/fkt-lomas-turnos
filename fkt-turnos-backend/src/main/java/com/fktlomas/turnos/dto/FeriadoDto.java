package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Feriado;

import java.time.LocalDate;

public record FeriadoDto(Long id, LocalDate fecha, String nombre, String tipo) {
    public static FeriadoDto de(Feriado f) {
        return new FeriadoDto(f.getId(), f.getFecha(), f.getNombre(), f.getTipo().name());
    }
}
