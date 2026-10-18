package com.fktlomas.turnos.controller;

import tools.jackson.databind.ObjectMapper;
import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.EvolucionClinicaRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El código de cada endpoint filtra correctamente por rol y por medicoId (verificado a mano
 * al revisar MedicoPortalService/ContadorCajaController), pero no había ningún test que
 * probara la garantía end-to-end: que un MEDICO nunca puede pegarle a /api/admin/** o
 * /api/contador/**, que un CONTADOR nunca puede pegarle a /api/medico/**, y que un médico
 * jamás ve la historia clínica que cargó otro médico para el mismo paciente. Esta última es
 * la peor clase de bug posible en un sistema con historias clínicas de por medio — sin este
 * test, esa garantía dependía únicamente de que nadie la rompiera sin querer en un refactor.
 *
 * Login real vía /api/auth/login (no @WithMockUser) para probar la cadena completa del
 * JwtAuthFilter tal como la ve un cliente real, igual que AuthControllerTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:autorizacion-roles-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        // Este archivo crea una cuenta de secretaria de verdad vía /api/admin/secretarias — sin
        // esto le pegaría a la API de HaveIBeenPwned en cada corrida (ver PasswordFiltradaService).
        "app.seguridad.chequeo-password-filtrada=false"
})
class AutorizacionRolesTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private TurnoRepository turnoRepository;
    @Autowired
    private EvolucionClinicaRepository evolucionClinicaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String CLAVE = "Segura123!";

    @Test
    void unMedicoNoPuedeAccederAEndpointsDeAdminNiDeContador() throws Exception {
        String token = loginComo(crearUsuario("medico.a@test.com", Role.MEDICO));

        mockMvc.perform(autenticado(get("/api/admin/resumen"), token)).andExpect(status().isForbidden());
        mockMvc.perform(autenticado(get("/api/contador/caja/resumen"), token)).andExpect(status().isForbidden());
    }

    @Test
    void unContadorNoPuedeAccederAEndpointsDeAdminNiDeMedico() throws Exception {
        // No hace falta el 2FA acá: ROLE_CONTADOR_MFA_PENDIENTE tampoco matchea
        // hasRole("ADMIN")/hasRole("MEDICO"), así que el 403 se prueba igual sin activarlo.
        String token = loginComo(crearUsuario("contador.a@test.com", Role.CONTADOR));

        mockMvc.perform(autenticado(get("/api/admin/resumen"), token)).andExpect(status().isForbidden());
        mockMvc.perform(autenticado(get("/api/medico/agenda-hoy"), token)).andExpect(status().isForbidden());
    }

    @Test
    void unContadorSinMfaActivadoNoPuedeVerLaCajaAunqueLaContraseñaSeaCorrecta() throws Exception {
        // El CONTADOR ve toda la plata del centro — a diferencia de médico/admin, acá el 2FA
        // no es opcional: sin activarlo, ni siquiera con la contraseña correcta se llega a
        // /api/contador/**. Este usuario se crea sin tocar mfaHabilitado (queda en false).
        Usuario contadorSinMfa = crearUsuario("contador.sinmfa@test.com", Role.CONTADOR);
        String token = loginComo(contadorSinMfa);

        mockMvc.perform(autenticado(get("/api/contador/caja/resumen"), token)).andExpect(status().isForbidden());
    }

    @Test
    void unContadorRecuperaAccesoInmediatamenteDespuesDeActivarMfaSinTenerQueVolverALoguearse() throws Exception {
        Usuario contador = crearUsuario("contador.activando@test.com", Role.CONTADOR);
        String token = loginComo(contador);

        mockMvc.perform(autenticado(get("/api/contador/caja/resumen"), token)).andExpect(status().isForbidden());

        // Simula que activó el 2FA (el flujo real es /api/mi-cuenta/mfa/**, disponible para
        // cualquier cuenta logueada incluida esta "pendiente" — acá se fuerza el estado final
        // directo para no reimplementar TOTP en el test).
        contador.setMfaHabilitado(true);
        usuarioRepository.save(contador);

        // Mismo token: AppUserDetails se reconstruye desde la base en cada request
        // (JwtAuthFilter), así que no hace falta un login nuevo para que el rol cambie.
        mockMvc.perform(autenticado(get("/api/contador/caja/resumen"), token)).andExpect(status().isOk());
    }

    @Test
    void unAdminOUnaSecretariaSinMfaActivadoNoPuedenUsarElPanelAunqueLaContraseñaSeaCorrecta() throws Exception {
        // Mismo mecanismo que el CONTADOR de arriba, extendido a ADMIN y SECRETARIA (ver
        // AppUserDetails.ROLES_MFA_OBLIGATORIO): sin 2FA activado, ninguno de los dos llega
        // a /api/admin/** aunque el login en sí (usuario + contraseña) sea correcto.
        String tokenAdmin = loginComo(crearUsuario("admin.sinmfa@test.com", Role.ADMIN));
        String tokenSecretaria = loginComo(crearUsuario("secretaria.sinmfa@test.com", Role.SECRETARIA));

        mockMvc.perform(autenticado(get("/api/admin/resumen"), tokenAdmin)).andExpect(status().isForbidden());
        mockMvc.perform(autenticado(get("/api/admin/resumen"), tokenSecretaria)).andExpect(status().isForbidden());
    }

    @Test
    void unAdminRecuperaAccesoInmediatamenteDespuesDeActivarMfaSinTenerQueVolverALoguearse() throws Exception {
        Usuario admin = crearUsuario("admin.activando@test.com", Role.ADMIN);
        String token = loginComo(admin);

        mockMvc.perform(autenticado(get("/api/admin/resumen"), token)).andExpect(status().isForbidden());

        // Mismo truco que con el contador: se fuerza el estado final directo en vez de
        // reimplementar TOTP acá, ver /api/mi-cuenta/mfa/** para el flujo real.
        admin.setMfaHabilitado(true);
        usuarioRepository.save(admin);

        mockMvc.perform(autenticado(get("/api/admin/resumen"), token)).andExpect(status().isOk());
    }

    @Test
    void unAdminOUnaSecretariaNoPuedenDesactivarSu2FAUnaVezActivado() throws Exception {
        // "Obligatorio" tiene que valer también para el camino de vuelta: si se pudiera
        // desactivar por autoservicio, la garantía de arriba (sin 2FA no hay acceso) sería
        // solo una fricción de un solo uso, no una obligación real.
        // Login primero (mientras el 2FA todavía está deshabilitado, así el login en sí no
        // exige un código) y recién después se activa el 2FA a mano, mismo truco que en
        // unAdminRecuperaAccesoInmediatamenteDespuesDeActivarMfaSinTenerQueVolverALoguearse.
        Usuario admin = crearUsuario("admin.condesactivar@test.com", Role.ADMIN);
        String token = loginComo(admin);
        habilitarMfa(admin);

        mockMvc.perform(autenticado(post("/api/mi-cuenta/mfa/desactivar"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"000000"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaSecretariaPuedeAccederAAgendaYMensajesPeroNoAMedicosNiCajaNiKinesiologiaClinica() throws Exception {
        // Con MFA activado: ADMIN/SECRETARIA son roles obligatorios (ver AppUserDetails), así
        // que sin esto la cuenta quedaría en ROLE_SECRETARIA_MFA_PENDIENTE y ni siquiera vería
        // la agenda — eso ya lo cubre el test de arriba, este prueba la autorización fina. Login
        // primero (con el 2FA todavía deshabilitado) y se activa recién después, mismo truco
        // que en unAdminRecuperaAccesoInmediatamenteDespuesDeActivarMfaSinTenerQueVolverALoguearse.
        Usuario secretaria = crearUsuario("secretaria.a@test.com", Role.SECRETARIA);
        String token = loginComo(secretaria);
        habilitarMfa(secretaria);

        // Sí: agenda / reserva telefónica / cola de espera — el trabajo diario de la secretaria.
        mockMvc.perform(autenticado(get("/api/admin/turnos")
                        .param("desde", LocalDate.now().toString())
                        .param("hasta", LocalDate.now().toString()), token))
                .andExpect(status().isOk());

        // Puede VER médicos (la agenda los necesita para el filtro) pero no darlos de alta.
        mockMvc.perform(autenticado(get("/api/admin/medicos"), token)).andExpect(status().isOk());
        mockMvc.perform(autenticado(post("/api/admin/medicos"), token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Dr. Test","especialidadId":1,"duracionTurnoMinutos":30}
                                """))
                .andExpect(status().isForbidden());
        // No: caja, kinesiología (decisiones clínicas o de negocio).
        mockMvc.perform(autenticado(get("/api/admin/caja/resumen"), token)).andExpect(status().isForbidden());
        mockMvc.perform(autenticado(put("/api/admin/kinesiologia/planes/1/validar"), token)).andExpect(status().isForbidden());
        // No: la plantilla/política de recordatorios (sí puede ver y reenviar los recordatorios en sí).
        mockMvc.perform(autenticado(get("/api/admin/recordatorios/configuracion"), token)).andExpect(status().isForbidden());
        // No: gestión de otras cuentas de staff.
        mockMvc.perform(autenticado(get("/api/admin/secretarias"), token)).andExpect(status().isForbidden());
        // No: el resumen de arranque del admin trae ingresosHoy/cajaFisica (AdminResumenDto).
        mockMvc.perform(autenticado(get("/api/admin/resumen"), token)).andExpect(status().isForbidden());
        // Sí: los contadores del menú lateral (sin datos de caja), que su panel pide al entrar.
        mockMvc.perform(autenticado(get("/api/admin/resumen/pendientes"), token)).andExpect(status().isOk());
        // Sí: la bandeja del botón de arrepentimiento.
        mockMvc.perform(autenticado(get("/api/admin/arrepentimientos"), token)).andExpect(status().isOk());
    }

    @Test
    void unPedidoPublicoMalArmadoDevuelve400YNo401() throws Exception {
        // Antes, al faltar un parámetro Spring reenviaba a /error, que exigía login: la respuesta
        // era un 401 "sesión vencida" que no tenía nada que ver con el problema real.
        mockMvc.perform(get("/api/medicos/1/proxima-disponibilidad")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/medicos/abc/horarios")).andExpect(status().isBadRequest());
    }

    @Test
    void unAdminPuedeCrearDesactivarYRevocarLasSesionesDeUnaSecretaria() throws Exception {
        Usuario admin = crearUsuario("admin.gestor@test.com", Role.ADMIN);
        String tokenAdmin = loginComo(admin);
        habilitarMfa(admin);

        String respuestaCreacion = mockMvc.perform(autenticado(post("/api/admin/secretarias"), tokenAdmin)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombreCompleto":"Secretaria Nueva","email":"secretaria.nueva@test.com","password":"%s"}
                                """.formatted(CLAVE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long secretariaId = objectMapper.readTree(respuestaCreacion).get("id").asLong();

        // La cuenta recién creada puede loguearse con normalidad, pero como cualquier
        // ADMIN/SECRETARIA nueva todavía no tiene el 2FA obligatorio activado — se lo activamos
        // acá (después del login, no antes: el login en sí ya exigiría un código) para poder
        // probar el resto del flujo (creación/desactivación/revocación).
        Usuario secretariaNueva = usuarioRepository.findById(secretariaId).orElseThrow();
        String tokenSecretaria = loginComo(secretariaNueva);
        habilitarMfa(secretariaNueva);
        mockMvc.perform(autenticado(get("/api/admin/turnos")
                        .param("desde", LocalDate.now().toString())
                        .param("hasta", LocalDate.now().toString()), tokenSecretaria))
                .andExpect(status().isOk());

        // El "iat" del JWT y sessionsRevokedAt se comparan truncados a segundos (mismo criterio
        // que passwordChangedAt en JwtAuthFilter, ver su comentario); sin este margen, si login
        // y desactivación caen en el mismo segundo de reloj (muy probable en un test rápido) la
        // desactivación no invalidaría el token ya emitido.
        Thread.sleep(1100);

        // El admin la desactiva: el token ya emitido deja de servir de inmediato (no hace
        // falta esperar a que expire), y un login nuevo también queda bloqueado.
        mockMvc.perform(autenticado(put("/api/admin/usuarios/" + secretariaId + "/desactivar"), tokenAdmin))
                .andExpect(status().isOk());
        mockMvc.perform(autenticado(get("/api/admin/turnos")
                        .param("desde", LocalDate.now().toString())
                        .param("hasta", LocalDate.now().toString()), tokenSecretaria))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"secretaria.nueva@test.com","password":"%s"}
                                """.formatted(CLAVE)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revocarMisSesionesInvalidaElTokenActual() throws Exception {
        String token = loginComo(crearUsuario("paciente.sesiones@test.com", Role.PACIENTE));

        mockMvc.perform(autenticado(get("/api/mi-cuenta"), token)).andExpect(status().isOk());
        Thread.sleep(1100); // ver comentario equivalente más arriba sobre la precisión de segundos del "iat"
        mockMvc.perform(autenticado(put("/api/mi-cuenta/revocar-sesiones"), token)).andExpect(status().isOk());
        mockMvc.perform(autenticado(get("/api/mi-cuenta"), token)).andExpect(status().isUnauthorized());
    }

    @Test
    void unPacienteNoPuedeAccederANingunPanelInterno() throws Exception {
        String token = loginComo(crearUsuario("paciente.a@test.com", Role.PACIENTE));

        mockMvc.perform(autenticado(get("/api/admin/resumen"), token)).andExpect(status().isForbidden());
        mockMvc.perform(autenticado(get("/api/medico/agenda-hoy"), token)).andExpect(status().isForbidden());
        mockMvc.perform(autenticado(get("/api/contador/caja/resumen"), token)).andExpect(status().isForbidden());
    }

    @Test
    void sinTokenLosEndpointsInternosDevuelven401() throws Exception {
        mockMvc.perform(get("/api/admin/resumen")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/medico/agenda-hoy")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/contador/caja/resumen")).andExpect(status().isUnauthorized());
    }

    @Test
    void unMedicoNuncaVeLaEvolucionClinicaQueCargoOtroMedicoParaElMismoPaciente() throws Exception {
        Medico medicoA = crearMedico();
        Medico medicoB = crearMedico();
        Usuario paciente = crearUsuario("paciente.compartido@test.com", Role.PACIENTE);

        Turno turnoDeA = new Turno();
        turnoDeA.setMedico(medicoA);
        turnoDeA.setPaciente(paciente);
        turnoDeA.setFecha(LocalDate.now());
        turnoDeA.setHoraInicio(LocalTime.of(10, 0));
        turnoDeA.setHoraFin(LocalTime.of(10, 30));
        turnoDeA.setEstado(EstadoTurno.RESERVADO);
        turnoDeA = turnoRepository.save(turnoDeA);

        EvolucionClinica evolucionDeA = new EvolucionClinica();
        evolucionDeA.setPaciente(paciente);
        evolucionDeA.setMedico(medicoA);
        evolucionDeA.setTurno(turnoDeA);
        evolucionDeA.setTexto("Evolución confidencial cargada por el médico A");
        evolucionClinicaRepository.save(evolucionDeA);

        String tokenB = loginComo(medicoB.getUsuario());

        mockMvc.perform(autenticado(get("/api/medico/pacientes/" + paciente.getId() + "/evoluciones"), tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private Usuario crearUsuario(String email, Role role) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setDni(String.valueOf((int) (Math.random() * 90_000_000) + 10_000_000));
        usuario.setNombreCompleto("Usuario Test " + role);
        usuario.setTelefono("1122334455");
        usuario.setGenero(Genero.PREFIERO_NO_DECIR);
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash(passwordEncoder.encode(CLAVE));
        usuario.setRole(role);
        return usuarioRepository.save(usuario);
    }

    private Usuario habilitarMfa(Usuario usuario) {
        usuario.setMfaHabilitado(true);
        return usuarioRepository.save(usuario);
    }

    private Medico crearMedico() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Usuario cuenta = crearUsuario("medico" + System.nanoTime() + "@test.com", Role.MEDICO);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Test " + System.nanoTime());
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        medico.setUsuario(cuenta);
        return medicoRepository.save(medico);
    }

    private String loginComo(Usuario usuario) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(usuario.getEmail(), CLAVE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(respuesta).get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder autenticado(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
