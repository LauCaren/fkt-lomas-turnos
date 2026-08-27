package com.fktlomas.turnos.dto;

import jakarta.validation.constraints.Size;

public record ReservarTurnoRequest(
        @Size(max = 500) String motivoConsulta
) {
}
