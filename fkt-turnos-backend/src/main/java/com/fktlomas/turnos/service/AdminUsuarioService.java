package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.admin.CuentaStaffDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Ciclo de vida de cuentas de staff (MEDICO/CONTADOR/SECRETARIA) — activar/desactivar y
 * revocar sesiones. Genérico a propósito: antes de esto no había ninguna forma de dar de baja
 * a un médico o contador tampoco, y el mecanismo es el mismo para los tres roles. No aplica a
 * PACIENTE (esa cuenta se maneja por otra vía — ver la solicitud de baja de "mi cuenta") ni a
 * ADMIN (un admin no puede desactivar su propia cuenta ni la de otro admin desde acá, para
 * evitar quedarse sin acceso).
 */
@Service
@RequiredArgsConstructor
public class AdminUsuarioService {

    private static final String AUDITORIA_TIPO = "CUENTA_STAFF";

    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Transactional
    public CuentaStaffDto activar(Long id, Usuario actor) {
        Usuario usuario = obtenerCuentaStaff(id);
        CuentaStaffDto antes = CuentaStaffDto.de(usuario);
        usuario.setActivo(true);
        CuentaStaffDto despues = CuentaStaffDto.de(usuarioRepository.save(usuario));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "ACTIVAR", antes, despues);
        return despues;
    }

    @Transactional
    public CuentaStaffDto desactivar(Long id, Usuario actor) {
        Usuario usuario = obtenerCuentaStaff(id);
        if (usuario.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("No podés desactivar tu propia cuenta");
        }
        CuentaStaffDto antes = CuentaStaffDto.de(usuario);
        usuario.setActivo(false);
        usuario.setSessionsRevokedAt(Instant.now());
        CuentaStaffDto despues = CuentaStaffDto.de(usuarioRepository.save(usuario));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "DESACTIVAR", antes, despues);
        return despues;
    }

    @Transactional
    public CuentaStaffDto revocarSesiones(Long id, Usuario actor) {
        Usuario usuario = obtenerCuentaStaff(id);
        usuario.setSessionsRevokedAt(Instant.now());
        CuentaStaffDto despues = CuentaStaffDto.de(usuarioRepository.save(usuario));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, id, "REVOCAR_SESIONES", null, despues);
        return despues;
    }

    private Usuario obtenerCuentaStaff(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cuenta no encontrada"));
        if (usuario.getRole() == Role.PACIENTE || usuario.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Esta operación es solo para cuentas de staff (médico, contador o secretaria)");
        }
        return usuario;
    }
}
