package com.fktlomas.turnos.dto.admin;

import com.fktlomas.turnos.domain.ListaEspera;

import java.time.Instant;

/**
 * Vista de la lista de espera para la secretaria: quién está anotado para un médico/día
 * puntual, para ofrecerle el turno directo cuando se libera un horario ahí (punto 11 del
 * pedido de UX). `createdAt` es lo único que hay para ordenar — no existe un campo de
 * prioridad en el dominio, no se inventa uno.
 */
public record ListaEsperaAdminDto(
        Long id,
        Long pacienteId,
        String pacienteNombre,
        String pacienteTelefono,
        String pacienteDni,
        Instant createdAt
) {
    public static ListaEsperaAdminDto de(ListaEspera le) {
        return new ListaEsperaAdminDto(
                le.getId(),
                le.getPaciente().getId(),
                le.getPaciente().getNombreCompleto(),
                le.getPaciente().getTelefono(),
                le.getPaciente().getDni(),
                le.getCreatedAt()
        );
    }
}
