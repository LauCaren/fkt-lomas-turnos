package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.admin.CambiarPasswordRequest;
import com.fktlomas.turnos.dto.auth.AuthResponse;
import com.fktlomas.turnos.dto.auth.LoginRequest;
import com.fktlomas.turnos.dto.auth.RegistroRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse registrarPaciente(RegistroRequest request) {
        if (request.pareceSpam()) {
            throw new IllegalArgumentException("No pudimos procesar el registro");
        }
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictoDeDatosException("Ya existe una cuenta registrada con ese email");
        }
        if (usuarioRepository.existsByDni(request.dni())) {
            throw new ConflictoDeDatosException("Ya existe una cuenta registrada con ese DNI");
        }

        Usuario usuario = new Usuario();
        usuario.setDni(request.dni());
        usuario.setObraSocial(request.obraSocial());
        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setEmail(request.email());
        usuario.setTelefono(request.telefono());
        usuario.setGenero(request.genero());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRole(Role.PACIENTE);
        usuario.setAceptaPoliticaPrivacidad(request.aceptaPoliticaPrivacidad());
        usuarioRepository.save(usuario);

        return new AuthResponse(jwtService.generarToken(usuario), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras autenticar"));
        return new AuthResponse(jwtService.generarToken(usuario), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRole().name());
    }

    @Transactional
    public void cambiarPassword(Usuario usuario, CambiarPasswordRequest request) {
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual no es correcta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuarioRepository.save(usuario);
    }

    public static Usuario usuarioActual(Object principal) {
        if (principal instanceof AppUserDetails appUserDetails) {
            return appUserDetails.getUsuario();
        }
        throw new IllegalStateException("No hay un usuario autenticado válido");
    }
}
