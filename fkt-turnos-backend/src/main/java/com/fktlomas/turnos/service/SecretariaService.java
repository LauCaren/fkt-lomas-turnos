package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.admin.CrearSecretariaRequest;
import com.fktlomas.turnos.dto.admin.CuentaStaffDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Alta de cuentas de secretaria — mismo patrón que MedicoService.crearOActualizarCredenciales
 * (unicidad de email, hash de password, setRole), pero sin vínculo a un Medico: una secretaria
 * es una cuenta de staff "suelta", no ligada a ninguna otra entidad del dominio.
 */
@Service
@RequiredArgsConstructor
public class SecretariaService {

    private static final String AUDITORIA_TIPO = "CUENTA_STAFF";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;
    private final PasswordFiltradaService passwordFiltradaService;

    @Transactional(readOnly = true)
    public List<CuentaStaffDto> listar() {
        return usuarioRepository.findByRoleOrderByNombreCompletoAsc(Role.SECRETARIA).stream()
                .map(CuentaStaffDto::de).toList();
    }

    @Transactional
    public CuentaStaffDto crear(CrearSecretariaRequest request, Usuario actor) {
        String emailNormalizado = request.email().trim().toLowerCase();
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            throw new ConflictoDeDatosException("Ya existe una cuenta con ese email");
        }
        passwordFiltradaService.verificar(request.password());

        Usuario usuario = new Usuario();
        usuario.setRole(Role.SECRETARIA);
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setEmail(emailNormalizado);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setAceptaPoliticaPrivacidad(true);
        CuentaStaffDto creada = CuentaStaffDto.de(usuarioRepository.save(usuario));
        auditoriaService.registrar(actor, AUDITORIA_TIPO, creada.id(), "CREAR_SECRETARIA", null, creada);
        return creada;
    }
}
