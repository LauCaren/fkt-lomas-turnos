package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.admin.CambiarPasswordRequest;
import com.fktlomas.turnos.dto.admin.IniciarMfaResponse;
import com.fktlomas.turnos.dto.auth.AuthResponse;
import com.fktlomas.turnos.dto.auth.LoginRequest;
import com.fktlomas.turnos.dto.auth.OlvidePasswordRequest;
import com.fktlomas.turnos.dto.auth.RegistroRequest;
import com.fktlomas.turnos.dto.auth.RestablecerPasswordRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.DemasiadosIntentosException;
import com.fktlomas.turnos.exception.MfaRequeridaException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.security.JwtService;
import com.fktlomas.turnos.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MINUTOS_VALIDEZ_TOKEN_RESET = 60;
    private static final int MAX_REGISTROS_POR_HORA = 5;
    private static final int MAX_OLVIDE_PASSWORD_POR_HORA = 5;
    // Una sola instancia reusada: SecureRandom es thread-safe y crear una nueva por llamada
    // tiene un costo de inicialización/sembrado innecesario.
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Set<Role> ROLES_ALERTA_HORARIO = EnumSet.of(Role.ADMIN, Role.SECRETARIA, Role.CONTADOR);
    // Por usuario, no global: a diferencia del job de recordatorios (una sola corrida), acá
    // cada persona que loguea fuera de horario merece su propio aviso — el enfriamiento es
    // solo para no mandar un mail por cada request si alguien queda logueando en loop (ej. un
    // script de monitoreo con credenciales de staff, o varios intentos fallidos de MFA).
    private static final Duration ENFRIAMIENTO_ALERTA_POR_USUARIO = Duration.ofHours(1);
    private final ConcurrentHashMap<Long, Instant> ultimaAlertaHorarioPorUsuario = new ConcurrentHashMap<>();

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final TotpService totpService;
    private final EmailService emailService;
    private final LoginRateLimiterService loginRateLimiterService;
    private final RateLimiterService rateLimiterService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.seguridad.horario-inicio}")
    private LocalTime horarioInicio;

    @Value("${app.seguridad.horario-fin}")
    private LocalTime horarioFin;

    @Transactional
    public AuthResponse registrarPaciente(RegistroRequest request) {
        verificarLimite(request.email(), MAX_REGISTROS_POR_HORA);
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

        return new AuthResponse(jwtService.generarToken(usuario), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRole().name(), false);
    }

    public AuthResponse login(LoginRequest request) {
        loginRateLimiterService.verificarNoBloqueado(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            loginRateLimiterService.registrarFallo(request.email());
            throw e;
        }
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras autenticar"));

        if (usuario.isMfaHabilitado()) {
            if (request.codigoMfa() == null || request.codigoMfa().isBlank()) {
                throw new MfaRequeridaException();
            }
            if (!totpService.verificar(usuario.getMfaSecret(), request.codigoMfa())) {
                loginRateLimiterService.registrarFallo(request.email());
                throw new BadCredentialsException("Código de verificación incorrecto");
            }
        }

        loginRateLimiterService.registrarExito(request.email());
        alertarLoginFueraDeHorarioSiCorresponde(usuario);
        boolean mfaSetupRequerido = usuario.getRole() == Role.CONTADOR && !usuario.isMfaHabilitado();
        return new AuthResponse(jwtService.generarToken(usuario), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRole().name(), mfaSetupRequerido);
    }

    /**
     * Avisa por mail cuando personal de oficina (ADMIN/SECRETARIA/CONTADOR) inicia sesión
     * fuera del horario configurado (app.seguridad.horario-*). No bloquea el login — es una
     * alerta, no un control de acceso — y no aplica a MEDICO ni PACIENTE: los médicos entran
     * a horarios más variables (guardias, recuperar turnos) y un paciente puede sacar un turno
     * a cualquier hora sin que eso sea señal de nada.
     */
    private void alertarLoginFueraDeHorarioSiCorresponde(Usuario usuario) {
        if (!ROLES_ALERTA_HORARIO.contains(usuario.getRole())) {
            return;
        }
        ZonedDateTime ahora = ZonedDateTime.now(ZONA_CLINICA);
        LocalTime hora = ahora.toLocalTime();
        boolean dentroDeHorario = !hora.isBefore(horarioInicio) && hora.isBefore(horarioFin);
        if (dentroDeHorario) {
            return;
        }

        Instant ultima = ultimaAlertaHorarioPorUsuario.get(usuario.getId());
        if (ultima != null && Duration.between(ultima, Instant.now()).compareTo(ENFRIAMIENTO_ALERTA_POR_USUARIO) < 0) {
            return;
        }
        ultimaAlertaHorarioPorUsuario.put(usuario.getId(), Instant.now());
        emailService.notificarLoginFueraDeHorario(usuario, ahora);
    }

    @Transactional
    public void olvidePassword(OlvidePasswordRequest request) {
        verificarLimite(request.email(), MAX_OLVIDE_PASSWORD_POR_HORA);
        usuarioRepository.findByEmail(request.email()).ifPresent(usuario -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            usuario.setResetToken(token);
            usuario.setResetTokenExpira(OffsetDateTime.now().plusMinutes(MINUTOS_VALIDEZ_TOKEN_RESET));
            usuarioRepository.save(usuario);

            String link = frontendUrl + "/restablecer-password?token=" + token;
            emailService.enviarRecuperacionPassword(usuario, link);
        });
        // Siempre se responde igual, exista o no el email, para no filtrar qué emails están registrados.
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByResetToken(request.token())
                .orElseThrow(() -> new RecursoNoEncontradoException("El link de recuperación no es válido o ya se usó"));

        if (usuario.getResetTokenExpira() == null || usuario.getResetTokenExpira().isBefore(OffsetDateTime.now())) {
            throw new RecursoNoEncontradoException("El link de recuperación venció, pedí uno nuevo");
        }

        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuario.setPasswordChangedAt(Instant.now());
        usuario.setResetToken(null);
        usuario.setResetTokenExpira(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void cambiarPassword(Usuario usuario, CambiarPasswordRequest request) {
        if (!passwordEncoder.matches(request.passwordActual(), usuario.getPasswordHash())) {
            throw new BadCredentialsException("La contraseña actual no es correcta");
        }
        usuario.setPasswordHash(passwordEncoder.encode(request.passwordNueva()));
        usuario.setPasswordChangedAt(Instant.now());
        usuarioRepository.save(usuario);
    }

    @Transactional
    public IniciarMfaResponse iniciarMfa(Usuario usuario) {
        String secreto = totpService.generarSecreto();
        usuario.setMfaSecret(secreto);
        usuario.setMfaHabilitado(false);
        usuarioRepository.save(usuario);
        return new IniciarMfaResponse(secreto, totpService.otpAuthUri(secreto, usuario.getEmail()));
    }

    @Transactional
    public void activarMfa(Usuario usuario, String codigo) {
        if (usuario.getMfaSecret() == null) {
            throw new IllegalArgumentException("Primero generá un código QR desde 'Activar verificación en dos pasos'");
        }
        if (!totpService.verificar(usuario.getMfaSecret(), codigo)) {
            throw new IllegalArgumentException("El código no es correcto, probá de nuevo");
        }
        usuario.setMfaHabilitado(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void desactivarMfa(Usuario usuario, String codigo) {
        if (!usuario.isMfaHabilitado() || !totpService.verificar(usuario.getMfaSecret(), codigo)) {
            throw new IllegalArgumentException("El código no es correcto");
        }
        usuario.setMfaHabilitado(false);
        usuario.setMfaSecret(null);
        usuarioRepository.save(usuario);
    }

    public static Usuario usuarioActual(Object principal) {
        if (principal instanceof AppUserDetails appUserDetails) {
            return appUserDetails.getUsuario();
        }
        throw new IllegalStateException("No hay un usuario autenticado válido");
    }

    /**
     * Límite por email (además del límite por IP que ya aplica RateLimitingFilter a nivel
     * de todo el pedido): evita que alguien reparta los intentos entre varias IPs para
     * seguir apuntando a la misma cuenta/dirección.
     */
    private void verificarLimite(String email, int maxPorHora) {
        if (!rateLimiterService.permitir("auth-email|" + (email == null ? "" : email.trim().toLowerCase()), maxPorHora, Duration.ofHours(1))) {
            throw new DemasiadosIntentosException("Demasiados pedidos. Probá de nuevo más tarde.");
        }
    }
}
