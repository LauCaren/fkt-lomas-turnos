package com.fktlomas.turnos.dto.arrepentimiento;

import com.fktlomas.turnos.util.DatosPersonales;
import com.fktlomas.turnos.validation.Patrones;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Formulario público del "Botón de arrepentimiento". Solo lo mínimo para identificar la
 * operación y contestarle a la persona — la norma prohíbe exigir registro u otros trámites.
 */
public record ArrepentimientoRequest(
        @NotBlank(message = "Contanos tu nombre y apellido")
        @Size(max = 150)
        String nombreCompleto,

        @Pattern(regexp = "^$|\\d{7,8}", message = "El DNI debe tener 7 u 8 dígitos")
        String dni,

        @NotBlank(message = "Necesitamos un email para mandarte el código del trámite")
        @Email(message = "El email no es válido")
        @Size(max = 180)
        String email,

        @Pattern(regexp = Patrones.TELEFONO_OPCIONAL, message = "El teléfono no es válido")
        String telefono,

        @NotBlank(message = "Contanos qué turno o pago querés cancelar")
        @Size(max = 1000)
        String detalleOperacion,

        /** Campo honeypot: invisible para personas, si viene completo es un bot. */
        String sitioWeb
) {
    public ArrepentimientoRequest {
        email = DatosPersonales.normalizarEmail(email);
    }

    public boolean pareceSpam() {
        return sitioWeb != null && !sitioWeb.isBlank();
    }
}
