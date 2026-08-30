package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que carga la secretaria para reservar un turno en nombre de un paciente
 * que llamó por teléfono o WhatsApp (no tiene cuenta, o no la usa). Si el DNI ya
 * pertenece a un paciente existente se reutiliza esa cuenta y se ignoran el resto
 * de los campos.
 */
public record ReservarParaPacienteRequest(
        @NotBlank(message = "El DNI es obligatorio")
        @Pattern(regexp = "\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
        String dni,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150)
        String nombreCompleto,

        @NotBlank(message = "El teléfono es obligatorio")
        @Pattern(regexp = Patrones.TELEFONO, message = "El teléfono no es válido")
        String telefono,

        @NotBlank(message = "La obra social es obligatoria (usá 'Particular' si no tiene)")
        @Size(max = 100)
        String obraSocial,

        @Size(max = 180)
        String email,

        @Size(max = 500)
        String motivoConsulta
) {
}
