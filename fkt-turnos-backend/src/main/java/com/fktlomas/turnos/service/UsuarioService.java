package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.ActualizarPerfilRequest;
import com.fktlomas.turnos.dto.EvolucionPacienteDto;
import com.fktlomas.turnos.dto.ExportarDatosDto;
import com.fktlomas.turnos.dto.PerfilDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.repository.EvolucionClinicaRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final EvolucionClinicaRepository evolucionClinicaRepository;
    private final TurnoService turnoService;
    private final KinesiologiaService kinesiologiaService;

    @Transactional
    public PerfilDto actualizarPerfil(Usuario usuario, ActualizarPerfilRequest request) {
        if (!usuario.getEmail().equalsIgnoreCase(request.email())
                && usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictoDeDatosException("Ya existe una cuenta registrada con ese email");
        }

        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setEmail(request.email());
        usuario.setTelefono(request.telefono());
        usuario.setGenero(request.genero());
        usuario.setObraSocial(request.obraSocial());
        usuario.setCanalNotificacionPreferido(request.canalNotificacionPreferido());
        usuarioRepository.save(usuario);

        return PerfilDto.de(usuario);
    }

    /** "Cerrar todas mis sesiones" por autoservicio — invalida cualquier JWT emitido antes de ahora, incluido el que se está usando para este pedido (fuerza a loguearse de nuevo). */
    @Transactional
    public void revocarMisSesiones(Usuario usuario) {
        usuario.setSessionsRevokedAt(Instant.now());
        usuarioRepository.save(usuario);
    }

    /** Derecho de acceso a la propia historia clínica (Ley 26.529 art. 14) — a diferencia del médico, el paciente ve las evoluciones de todos los profesionales que lo atendieron. */
    @Transactional(readOnly = true)
    public List<EvolucionPacienteDto> misEvoluciones(Usuario paciente) {
        return evolucionClinicaRepository.findByPacienteIdOrderByCreatedAtDesc(paciente.getId())
                .stream().map(EvolucionPacienteDto::de).toList();
    }

    /** Derecho de acceso a los propios datos personales (Ley 25.326) — un bundle con todo lo que el sistema tiene sobre esta cuenta. */
    @Transactional(readOnly = true)
    public ExportarDatosDto exportarMisDatos(Usuario usuario) {
        return new ExportarDatosDto(
                Instant.now(),
                PerfilDto.de(usuario),
                turnoService.misTurnos(usuario.getId()),
                kinesiologiaService.misPlanes(usuario.getId()),
                misEvoluciones(usuario)
        );
    }
}
