package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.OrigenConsentimiento;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.admin.CambiarPasswordRequest;
import com.fktlomas.turnos.dto.admin.IniciarMfaResponse;
import com.fktlomas.turnos.dto.auth.AuthResponse;
import com.fktlomas.turnos.dto.auth.GoogleAuthRequest;
import com.fktlomas.turnos.dto.auth.GoogleAuthResponse;
import com.fktlomas.turnos.dto.auth.LoginRequest;
import com.fktlomas.turnos.dto.auth.OlvidePasswordRequest;
import com.fktlomas.turnos.dto.auth.RegistroRequest;
import com.fktlomas.turnos.dto.auth.RestablecerPasswordRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.DemasiadosIntentosException;
import com.fktlomas.turnos.exception.MfaRequeridaException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.security.GoogleTokenVerifierService;
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
    // Un solo mensaje para "email ya usado" y "DNI ya usado": con mensajes distintos, el
    // formulario de registro servía para averiguar si un DNI cualquiera es paciente del centro
    // (un dato de salud, sensible según la Ley 25.326). La persona real sabe qué hacer con este
    // mensaje igual: entrar o recuperar la contraseña.
    static final String MENSAJE_CUENTA_EXISTENTE =
            "Ya hay una cuenta con ese email o DNI. Si es tuya, ingresá o recuperá tu contraseña.";
    // Una sola instancia reusada: SecureRandom es thread-safe y crear una nueva por llamada
    // tiene un costo de inicialización/sembrado innecesario.
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Set<Role> ROLES_ALERTA_HORARIO = EnumSet.of(Role.ADMIN, Role.SECRETARIA, Role.CONTADOR);
    // Mantenido en sync a mano con AppUserDetails.ROLES_MFA_OBLIGATORIO. Coincide hoy con
    // ROLES_ALERTA_HORARIO, pero son conceptos distintos (uno avisa, el otro bloquea acceso)
    // que podrían divergir — por eso una constante propia en vez de reusar la de arriba.
    private static final Set<Role> ROLES_MFA_OBLIGATORIO = EnumSet.of(Role.ADMIN, Role.SECRETARIA, Role.CONTADOR);
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
    private final PasswordFiltradaService passwordFiltradaService;
    private final GoogleTokenVerifierService googleTokenVerifierService;

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
        if (usuarioRepository.existsByEmail(request.email()) || usuarioRepository.existsByDni(request.dni())) {
            throw new ConflictoDeDatosException(MENSAJE_CUENTA_EXISTENTE);
        }
        passwordFiltradaService.verificar(request.password());

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
        usuario.setOrigenConsentimiento(OrigenConsentimiento.WEB);
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

        verificarSegundoFactor(usuario, request.codigoMfa());

        loginRateLimiterService.registrarExito(request.email());
        alertarLoginFueraDeHorarioSiCorresponde(usuario);
        boolean mfaSetupRequerido = ROLES_MFA_OBLIGATORIO.contains(usuario.getRole()) && !usuario.isMfaHabilitado();
        return new AuthResponse(jwtService.generarToken(usuario), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRole().name(), mfaSetupRequerido);
    }

    /**
     * "Iniciar sesión con Google", solo para pacientes (el personal sigue entrando con
     * email/contraseña + 2FA obligatorio, ver ROLES_MFA_OBLIGATORIO — combinar eso con Google
     * complica el flujo de 2FA sin necesidad, y el personal ya tiene una cuenta creada por un
     * admin, no hace falta la conveniencia de "un click"). Tres casos según lo que ya exista:
     * 1) googleId conocido → login directo (con el mismo chequeo de MFA que login() de arriba).
     * 2) el email ya tiene cuenta por password → se vincula (Google ya verificó el email, así
     *    que confiar en esa verificación para linkear es seguro) y se loguea igual que el caso 1.
     * 3) ni lo uno ni lo otro → alta nueva. El DNI es inmutable una vez creado (ver MiCuentaPage
     *    en el frontend, que lo muestra deshabilitado) así que NO se crea la cuenta todavía si
     *    todavía faltan dni/obraSocial/teléfono/género — se le pide el resto al frontend primero
     *    (GoogleAuthResponse.requierePerfil) reenviando el mismo credential.
     */
    @Transactional
    public GoogleAuthResponse loginConGoogle(GoogleAuthRequest request) {
        GoogleTokenVerifierService.DatosGoogle datos = googleTokenVerifierService.verificar(request.credential());
        if (datos == null) {
            throw new BadCredentialsException("No pudimos verificar tu cuenta de Google");
        }
        String email = datos.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByGoogleId(datos.googleId()).orElse(null);
        if (usuario == null) {
            usuario = usuarioRepository.findByEmail(email).orElse(null);
            // El personal no entra con Google (ver el javadoc de arriba). Antes esto solo estaba
            // documentado: una cuenta de staff cuyo email fuera también una cuenta de Google
            // podía vincularse y entrar sin su contraseña. Se corta acá, antes de vincular nada.
            if (usuario != null && usuario.getRole() != Role.PACIENTE) {
                throw new BadCredentialsException("El personal del centro ingresa con su email y contraseña");
            }
            if (usuario != null) {
                usuario.setGoogleId(datos.googleId());
                usuarioRepository.save(usuario);
            }
        }

        if (usuario != null && usuario.getRole() != Role.PACIENTE) {
            throw new BadCredentialsException("El personal del centro ingresa con su email y contraseña");
        }
        return usuario != null
                ? iniciarSesionGoogle(usuario, request)
                : registrarPacienteConGoogle(datos, email, request);
    }

    private GoogleAuthResponse iniciarSesionGoogle(Usuario usuario, GoogleAuthRequest request) {
        loginRateLimiterService.verificarNoBloqueado(usuario.getEmail());
        verificarSegundoFactor(usuario, request.codigoMfa());
        loginRateLimiterService.registrarExito(usuario.getEmail());
        alertarLoginFueraDeHorarioSiCorresponde(usuario);
        boolean mfaSetupRequerido = ROLES_MFA_OBLIGATORIO.contains(usuario.getRole()) && !usuario.isMfaHabilitado();
        AuthResponse auth = new AuthResponse(jwtService.generarToken(usuario), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRole().name(), mfaSetupRequerido);
        return GoogleAuthResponse.sesion(auth);
    }

    private GoogleAuthResponse registrarPacienteConGoogle(GoogleTokenVerifierService.DatosGoogle datos, String email, GoogleAuthRequest request) {
        boolean perfilCompleto = request.dni() != null && !request.dni().isBlank()
                && request.obraSocial() != null && !request.obraSocial().isBlank()
                && request.telefono() != null && !request.telefono().isBlank()
                && request.genero() != null
                && Boolean.TRUE.equals(request.aceptaPoliticaPrivacidad());
        if (!perfilCompleto) {
            return GoogleAuthResponse.requierePerfil(datos.nombre(), email);
        }

        verificarLimite(email, MAX_REGISTROS_POR_HORA);
        if (request.pareceSpam()) {
            throw new IllegalArgumentException("No pudimos procesar el registro");
        }
        if (usuarioRepository.existsByEmail(email) || usuarioRepository.existsByDni(request.dni())) {
            throw new ConflictoDeDatosException(MENSAJE_CUENTA_EXISTENTE);
        }

        // Nunca se usa para loguear (esta cuenta entra por Google) — solo para que passwordHash,
        // que es NOT NULL en toda la tabla, tenga un valor válido que nadie puede adivinar.
        // "Olvidé mi contraseña" les sirve para fijar una de verdad si alguna vez la quieren.
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String passwordInutilizable = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        Usuario usuario = new Usuario();
        usuario.setDni(request.dni());
        usuario.setObraSocial(request.obraSocial());
        usuario.setNombreCompleto(datos.nombre() != null && !datos.nombre().isBlank() ? datos.nombre() : email);
        usuario.setEmail(email);
        usuario.setTelefono(request.telefono());
        usuario.setGenero(request.genero());
        usuario.setPasswordHash(passwordEncoder.encode(passwordInutilizable));
        usuario.setRole(Role.PACIENTE);
        usuario.setAceptaPoliticaPrivacidad(true);
        usuario.setOrigenConsentimiento(OrigenConsentimiento.WEB);
        usuario.setGoogleId(datos.googleId());
        usuarioRepository.save(usuario);

        AuthResponse auth = new AuthResponse(jwtService.generarToken(usuario), usuario.getNombreCompleto(), usuario.getEmail(), usuario.getRole().name(), false);
        return GoogleAuthResponse.sesion(auth);
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

            usuario.setResetToken(hashDeToken(token));
            usuario.setResetTokenExpira(OffsetDateTime.now().plusMinutes(MINUTOS_VALIDEZ_TOKEN_RESET));
            usuarioRepository.save(usuario);

            String link = frontendUrl + "/restablecer-password?token=" + token;
            emailService.enviarRecuperacionPassword(usuario, link);
        });
        // Siempre se responde igual, exista o no el email, para no filtrar qué emails están registrados.
    }

    @Transactional
    public void restablecerPassword(RestablecerPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByResetToken(hashDeToken(request.token()))
                .orElseThrow(() -> new RecursoNoEncontradoException("El link de recuperación no es válido o ya se usó"));

        if (usuario.getResetTokenExpira() == null || usuario.getResetTokenExpira().isBefore(OffsetDateTime.now())) {
            throw new RecursoNoEncontradoException("El link de recuperación venció, pedí uno nuevo");
        }
        passwordFiltradaService.verificar(request.passwordNueva());

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
        passwordFiltradaService.verificar(request.passwordNueva());
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
        if (ROLES_MFA_OBLIGATORIO.contains(usuario.getRole())) {
            throw new IllegalArgumentException("La verificación en dos pasos es obligatoria para tu rol y no se puede desactivar");
        }
        if (!usuario.isMfaHabilitado() || !totpService.verificar(usuario.getMfaSecret(), codigo)) {
            throw new IllegalArgumentException("El código no es correcto");
        }
        usuario.setMfaHabilitado(false);
        usuario.setMfaSecret(null);
        usuarioRepository.save(usuario);
    }

    /**
     * Segundo factor del login (con contraseña o con Google). Si la cuenta no tiene 2FA, no hace
     * nada. Si lo tiene: sin código pide el código (401 con requiereMfa), con un código
     * incorrecto cuenta como intento fallido, y con un código correcto pero ya usado antes
     * (mismo paso de 30 s o anterior) también lo rechaza — un código TOTP sirve una sola vez.
     */
    private void verificarSegundoFactor(Usuario usuario, String codigoMfa) {
        if (!usuario.isMfaHabilitado()) {
            return;
        }
        if (codigoMfa == null || codigoMfa.isBlank()) {
            throw new MfaRequeridaException();
        }
        long paso = totpService.pasoCoincidente(usuario.getMfaSecret(), codigoMfa.trim());
        boolean yaUsado = usuario.getMfaUltimoPaso() != null && paso <= usuario.getMfaUltimoPaso();
        if (paso < 0 || yaUsado) {
            loginRateLimiterService.registrarFallo(usuario.getEmail());
            throw new BadCredentialsException(yaUsado
                    ? "Ese código ya se usó. Esperá a que la app muestre uno nuevo."
                    : "Código de verificación incorrecto");
        }
        usuario.setMfaUltimoPaso(paso);
        usuarioRepository.save(usuario);
    }

    /**
     * SHA-256 en Base64 URL (43 caracteres, entra en la columna de 64). El token tiene 256 bits
     * aleatorios, así que no hace falta sal ni un hash lento: no hay diccionario posible.
     */
    static String hashDeToken(String token) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
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
