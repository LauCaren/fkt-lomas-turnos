package com.fktlomas.turnos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.TurnoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cubre la garantía central de la feature: una reseña no se puede fabricar sin haber sido
 * atendido de verdad (turno propio + ATENDIDO), no se puede duplicar por turno, y solo llega
 * al público (GET /api/medicos/{id}/resenas) después de que un admin/secretaria la aprueba —
 * antes de eso, ni con la reseña ya cargada, aparece.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:resena-controller-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class ResenaControllerTest {

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
    private PasswordEncoder passwordEncoder;

    private static final String CLAVE = "Segura123!";

    @Test
    void noSePuedeCalificarUnTurnoPropioQueTodaviaNoFueAtendido() throws Exception {
        Medico medico = crearMedico();
        Usuario paciente = crearUsuario("paciente.sinatender@test.com", Role.PACIENTE);
        Turno turno = crearTurno(medico, paciente, EstadoAsistencia.PENDIENTE);
        String token = loginComo(paciente);

        mockMvc.perform(autenticado(post("/api/turnos/" + turno.getId() + "/resena"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"puntaje":5,"comentario":"Excelente atención"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void noSePuedeCalificarElTurnoDeOtroPaciente() throws Exception {
        Medico medico = crearMedico();
        Usuario dueño = crearUsuario("paciente.dueño@test.com", Role.PACIENTE);
        Usuario otro = crearUsuario("paciente.otro@test.com", Role.PACIENTE);
        Turno turno = crearTurno(medico, dueño, EstadoAsistencia.ATENDIDO);
        String tokenOtro = loginComo(otro);

        mockMvc.perform(autenticado(post("/api/turnos/" + turno.getId() + "/resena"), tokenOtro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"puntaje":5,"comentario":"no es mi turno"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void noSePuedeCalificarElMismoTurnoDosVeces() throws Exception {
        Medico medico = crearMedico();
        Usuario paciente = crearUsuario("paciente.doble@test.com", Role.PACIENTE);
        Turno turno = crearTurno(medico, paciente, EstadoAsistencia.ATENDIDO);
        String token = loginComo(paciente);

        mockMvc.perform(autenticado(post("/api/turnos/" + turno.getId() + "/resena"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"puntaje":4,"comentario":"Muy bien"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(autenticado(post("/api/turnos/" + turno.getId() + "/resena"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"puntaje":2,"comentario":"cambié de opinión"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unaResenaSoloApareceEnPublicoDespuesDeQueUnAdminLaApruebe() throws Exception {
        Medico medico = crearMedico();
        Usuario paciente = crearUsuario("paciente.moderacion@test.com", Role.PACIENTE);
        Turno turno = crearTurno(medico, paciente, EstadoAsistencia.ATENDIDO);
        String tokenPaciente = loginComo(paciente);

        String respuesta = mockMvc.perform(autenticado(post("/api/turnos/" + turno.getId() + "/resena"), tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"puntaje":5,"comentario":"Un golazo de atención"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long resenaId = objectMapper.readTree(respuesta).get("id").asLong();

        // Pendiente: no aparece en el endpoint público todavía.
        mockMvc.perform(get("/api/medicos/" + medico.getId() + "/resenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0))
                .andExpect(jsonPath("$.resenas").isEmpty());

        // Login antes de activar el 2FA (si no, el login en sí ya exigiría un código) — mismo
        // truco que en AutorizacionRolesTest: el rol se recalcula en el siguiente request sin
        // necesidad de un login nuevo.
        Usuario admin = crearUsuario("admin.moderador@test.com", Role.ADMIN);
        String tokenAdmin = loginComo(admin);
        habilitarMfa(admin);
        mockMvc.perform(autenticado(put("/api/admin/resenas/" + resenaId + "/aprobar"), tokenAdmin))
                .andExpect(status().isOk());

        // Aprobada: ahora sí, y sin exponer el apellido completo del paciente.
        mockMvc.perform(get("/api/medicos/" + medico.getId() + "/resenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(1))
                .andExpect(jsonPath("$.promedio").value(5.0))
                .andExpect(jsonPath("$.resenas[0].comentario").value("Un golazo de atención"))
                .andExpect(jsonPath("$.resenas[0].pacienteNombreCorto").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Test "))));
    }

    @Test
    void unaResenaRechazadaNuncaLlegaAlPublicoYQuedaConMotivo() throws Exception {
        Medico medico = crearMedico();
        Usuario paciente = crearUsuario("paciente.rechazo@test.com", Role.PACIENTE);
        Turno turno = crearTurno(medico, paciente, EstadoAsistencia.ATENDIDO);
        String tokenPaciente = loginComo(paciente);

        String respuesta = mockMvc.perform(autenticado(post("/api/turnos/" + turno.getId() + "/resena"), tokenPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"puntaje":1,"comentario":"contenido inapropiado"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long resenaId = objectMapper.readTree(respuesta).get("id").asLong();

        Usuario admin = crearUsuario("admin.rechaza@test.com", Role.ADMIN);
        String tokenAdmin = loginComo(admin);
        habilitarMfa(admin);
        mockMvc.perform(autenticado(put("/api/admin/resenas/" + resenaId + "/rechazar"), tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"motivo":"Lenguaje inapropiado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RECHAZADA"))
                .andExpect(jsonPath("$.motivoRechazo").value("Lenguaje inapropiado"));

        mockMvc.perform(get("/api/medicos/" + medico.getId() + "/resenas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidad").value(0));
    }

    private Usuario habilitarMfa(Usuario usuario) {
        usuario.setMfaHabilitado(true);
        return usuarioRepository.save(usuario);
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

    private Medico crearMedico() {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre("Especialidad " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto("Dr. Test " + System.nanoTime());
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medicoRepository.save(medico);
    }

    private Turno crearTurno(Medico medico, Usuario paciente, EstadoAsistencia estadoAsistencia) {
        Turno turno = new Turno();
        turno.setMedico(medico);
        turno.setPaciente(paciente);
        turno.setFecha(LocalDate.now());
        turno.setHoraInicio(LocalTime.of(10, 0));
        turno.setHoraFin(LocalTime.of(10, 30));
        turno.setEstado(EstadoTurno.RESERVADO);
        turno.setEstadoAsistencia(estadoAsistencia);
        return turnoRepository.save(turno);
    }

    private String loginComo(Usuario usuario) throws Exception {
        String respuesta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(usuario.getEmail(), CLAVE)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(respuesta).get("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }
}
