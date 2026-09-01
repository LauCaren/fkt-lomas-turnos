package com.fktlomas.turnos.dto.caja;

import com.fktlomas.turnos.domain.CategoriaGasto;

public record CategoriaGastoDto(Long id, String nombre) {
    public static CategoriaGastoDto de(CategoriaGasto c) {
        return new CategoriaGastoDto(c.getId(), c.getNombre());
    }
}
