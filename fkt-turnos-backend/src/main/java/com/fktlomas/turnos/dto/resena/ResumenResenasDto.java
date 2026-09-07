package com.fktlomas.turnos.dto.resena;

import java.util.List;

public record ResumenResenasDto(
        double promedio,
        long cantidad,
        List<ResenaPublicaDto> resenas
) {
}
