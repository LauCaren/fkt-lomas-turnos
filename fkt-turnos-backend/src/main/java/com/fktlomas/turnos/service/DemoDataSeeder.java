package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Carga una base de ejemplo completa (pacientes, turnos en todos los estados, caja, kinesiología,
 * mensajes, etc.) para poder probar el sistema entero sin cargar nada a mano. SOLO para
 * desarrollo local — apagado por defecto (ver app.demo-data.enabled), nunca se activa solo. No
 * se toca ningún dato real: corre después de Flyway, sobre especialidades/médicos/categorías de
 * gasto/feriados ya sembrados por las migraciones, no los duplica ni los reemplaza.
 *
 * Idempotente por las dudas (chequea si ya corrió antes de insertar nada), pero el uso previsto
 * es sobre una base recién creada (`docker compose down -v && docker compose up -d`).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private static final String PASSWORD_DEMO = "Demo1234!";
    private static final String EMAIL_SECRETARIA = "secretaria.demo@fktlomas.com";
    private static final String EMAIL_MEDICO = "medico.demo@fktlomas.com";
    private static final String EMAIL_CONTADOR = "contador.demo@fktlomas.com";
    private static final String EMAIL_PACIENTE = "paciente.demo@fktlomas.com";

    private final UsuarioRepository usuarioRepository;
    private final MedicoRepository medicoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final TurnoRepository turnoRepository;
    private final PlanKinesiologiaRepository planKinesiologiaRepository;
    private final EvolucionClinicaRepository evolucionClinicaRepository;
    private final IngresoRepository ingresoRepository;
    private final RetiroRepository retiroRepository;
    private final GastoRepository gastoRepository;
    private final CategoriaGastoRepository categoriaGastoRepository;
    private final MensajeContactoRepository mensajeContactoRepository;
    private final PostulacionRepository postulacionRepository;
    private final ListaEsperaRepository listaEsperaRepository;
    private final SolicitudBajaCuentaRepository solicitudBajaCuentaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (usuarioRepository.existsByEmail(EMAIL_SECRETARIA)) {
            log.info("DemoDataSeeder: ya hay datos de ejemplo cargados, no se hace nada.");
            return;
        }
        log.info("DemoDataSeeder: cargando datos de ejemplo...");

        // Solo valida que la especialidad exista (¿corrió Flyway?) — nada más del método la usa.
        especialidadRepository.findAll().stream()
                .filter(Especialidad::isEsKinesiologia).findFirst()
                .orElseThrow(() -> new IllegalStateException("No hay especialidad de kinesiología sembrada — ¿corrió Flyway?"));

        List<Medico> medicos = medicoRepository.findAll();
        Medico medicoDemo = buscarMedico(medicos, "Diego Palacios");
        Medico medicoDemo2 = buscarMedico(medicos, "Graciela Sanchez");
        Medico medicoDemo3 = buscarMedico(medicos, "Paola Luna");

        Usuario secretaria = crearStaff(EMAIL_SECRETARIA, "Secretaria de Prueba", Role.SECRETARIA);
        Usuario contador = crearStaff(EMAIL_CONTADOR, "Contador de Prueba", Role.CONTADOR);
        Usuario medicoUsuario = crearStaff(EMAIL_MEDICO, medicoDemo.getNombreCompleto(), Role.MEDICO);
        medicoDemo.setUsuario(medicoUsuario);
        medicoDemo.setMatricula("MP-45.812");
        medicoDemo.setAnosExperiencia(12);
        medicoDemo.setTarifaConsulta(new BigDecimal("15000"));
        medicoRepository.save(medicoDemo);
        medicoDemo2.setMatricula("MP-38.220");
        medicoDemo2.setAnosExperiencia(8);
        medicoDemo2.setTarifaConsulta(new BigDecimal("18000"));
        medicoDemo2.setCobraCopagoIoma(true);
        medicoRepository.save(medicoDemo2);

        List<Usuario> pacientes = crearPacientes();

        List<Turno> turnosDemo1 = generarAgenda(medicoDemo, pacientes);
        List<Turno> turnosDemo2 = generarAgenda(medicoDemo2, pacientes);
        List<Turno> todosLosTurnos = new ArrayList<>();
        todosLosTurnos.addAll(turnosDemo1);
        todosLosTurnos.addAll(turnosDemo2);

        cargarEvoluciones(todosLosTurnos, secretaria);
        cargarKinesiologia(kinesiologiaPlanPaciente(pacientes), medicoDemo, medicoDemo3, secretaria, turnosDemo1);
        cargarCaja(medicoDemo, medicoDemo2, secretaria, contador, turnosDemo1);
        cargarMensajesYPostulaciones();
        cargarListaEsperaYBaja(medicoDemo, pacientes);

        log.info("""

                ================================================================
                DemoDataSeeder: listo. Usuarios de prueba (todos con la misma
                contraseña "{}"):
                  ADMIN       fernando@fktlomas.com   (bootstrap — ver ADMIN_BOOTSTRAP_PASSWORD)
                  SECRETARIA  {}
                  MEDICO      {}
                  CONTADOR    {}   (2FA obligatoria: se configura en el primer login)
                  PACIENTE    {}
                ================================================================
                """, PASSWORD_DEMO, EMAIL_SECRETARIA, EMAIL_MEDICO, EMAIL_CONTADOR, EMAIL_PACIENTE);
    }

    private Medico buscarMedico(List<Medico> medicos, String apellidoNombre) {
        return medicos.stream()
                .filter(m -> m.getNombreCompleto().contains(apellidoNombre))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró el médico sembrado '" + apellidoNombre + "' — ¿corrió Flyway (V3)?"));
    }

    private Usuario crearStaff(String email, String nombre, Role role) {
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNombreCompleto(nombre);
        u.setTelefono("1122330000");
        u.setPasswordHash(passwordEncoder.encode(PASSWORD_DEMO));
        u.setRole(role);
        u.setAceptaPoliticaPrivacidad(true);
        return usuarioRepository.save(u);
    }

    private List<Usuario> crearPacientes() {
        record DatosPaciente(String dni, String nombre, String email, String telefono, Genero genero, String obraSocial) {
        }
        List<DatosPaciente> datos = List.of(
                new DatosPaciente("30111222", "Martina Fernández", EMAIL_PACIENTE, "1122334401", Genero.FEMENINO, "OSDE"),
                new DatosPaciente("28222333", "Lucas Gómez", "lucas.gomez@ejemplo.com", "1122334402", Genero.MASCULINO, "Swiss Medical"),
                new DatosPaciente("35333444", "Sofía Rodríguez", "sofia.rodriguez@ejemplo.com", "1122334403", Genero.FEMENINO, "IOMA"),
                new DatosPaciente("40444555", "Mateo Pérez", "mateo.perez@ejemplo.com", "1122334404", Genero.MASCULINO, "Particular"),
                new DatosPaciente("32555666", "Valentina López", "valentina.lopez@ejemplo.com", "1122334405", Genero.FEMENINO, "Galeno"),
                new DatosPaciente("29666777", "Joaquín Díaz", "joaquin.diaz@ejemplo.com", "1122334406", Genero.MASCULINO, "OSDE"),
                new DatosPaciente("38777888", "Camila Torres", "camila.torres@ejemplo.com", "1122334407", Genero.FEMENINO, "Particular"),
                new DatosPaciente("33888999", "Benjamín Ruiz", "benjamin.ruiz@ejemplo.com", "1122334408", Genero.MASCULINO, "Swiss Medical")
        );
        List<Usuario> resultado = new ArrayList<>();
        for (DatosPaciente d : datos) {
            Usuario u = new Usuario();
            u.setDni(d.dni());
            u.setNombreCompleto(d.nombre());
            u.setEmail(d.email());
            u.setTelefono(d.telefono());
            u.setGenero(d.genero());
            u.setObraSocial(d.obraSocial());
            u.setPasswordHash(passwordEncoder.encode(PASSWORD_DEMO));
            u.setRole(Role.PACIENTE);
            u.setAceptaPoliticaPrivacidad(true);
            resultado.add(usuarioRepository.save(u));
        }
        return resultado;
    }

    private static final List<LocalTime> HORARIOS = List.of(
            LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(11, 0),
            LocalTime.of(15, 0), LocalTime.of(15, 30), LocalTime.of(16, 0), LocalTime.of(16, 30), LocalTime.of(17, 0)
    );

    private static final List<String> MOTIVOS = List.of(
            "Control", "Primera consulta", "Dolor lumbar", "Seguimiento", "Rehabilitación post-quirúrgica", "Chequeo general"
    );

    /** Agenda de ~2 semanas para un médico: pasado (atendido/ausente/cancelado), hoy (cola de espera) y próximos días (reservado/disponible). */
    private List<Turno> generarAgenda(Medico medico, List<Usuario> pacientes) {
        List<Turno> creados = new ArrayList<>();
        int duracion = medico.getDuracionTurnoMinutos();
        int contadorPaciente = 0;

        // Pasado: últimos 6 días hábiles, 2 turnos por día (uno atendido, uno ausente/cancelado alternando).
        LocalDate cursor = LocalDate.now().minusDays(1);
        int diasCargados = 0;
        while (diasCargados < 6) {
            if (esHabil(cursor)) {
                Usuario p1 = pacientes.get(contadorPaciente++ % pacientes.size());
                Usuario p2 = pacientes.get(contadorPaciente++ % pacientes.size());
                creados.add(crearTurno(medico, cursor, HORARIOS.get(0), duracion, EstadoTurno.RESERVADO, p1,
                        EstadoAsistencia.ATENDIDO, ConfirmacionPrevia.CONFIRMADO));
                creados.add(crearTurno(medico, cursor, HORARIOS.get(1), duracion,
                        diasCargados % 3 == 0 ? EstadoTurno.CANCELADO : EstadoTurno.RESERVADO, p2,
                        diasCargados % 3 == 0 ? EstadoAsistencia.PENDIENTE : EstadoAsistencia.AUSENTE,
                        ConfirmacionPrevia.SIN_RESPUESTA));
                diasCargados++;
            }
            cursor = cursor.minusDays(1);
        }

        // Hoy: demuestra la cola de espera (LLEGO) y un turno ya atendido más temprano.
        LocalDate hoy = LocalDate.now();
        if (esHabil(hoy)) {
            Usuario pAtendidoHoy = pacientes.get(contadorPaciente++ % pacientes.size());
            Usuario pEnEspera = pacientes.get(contadorPaciente++ % pacientes.size());
            Usuario pProximo = pacientes.get(contadorPaciente++ % pacientes.size());
            creados.add(crearTurno(medico, hoy, HORARIOS.get(0), duracion, EstadoTurno.RESERVADO, pAtendidoHoy,
                    EstadoAsistencia.ATENDIDO, ConfirmacionPrevia.CONFIRMADO));
            creados.add(crearTurno(medico, hoy, HORARIOS.get(1), duracion, EstadoTurno.RESERVADO, pEnEspera,
                    EstadoAsistencia.LLEGO, ConfirmacionPrevia.CONFIRMADO));
            creados.add(crearTurno(medico, hoy, HORARIOS.get(5), duracion, EstadoTurno.RESERVADO, pProximo,
                    EstadoAsistencia.PENDIENTE, ConfirmacionPrevia.SIN_RESPUESTA));
            creados.add(crearTurno(medico, hoy, HORARIOS.get(6), duracion, EstadoTurno.DISPONIBLE, null,
                    EstadoAsistencia.PENDIENTE, ConfirmacionPrevia.SIN_RESPUESTA));
        }

        // Próximos 8 días hábiles: mezcla de reservado (con confirmación previa pendiente/rechazada
        // para mostrar esa pantalla) y disponible, dejando huecos para que se note que se puede reservar.
        cursor = hoy.plusDays(1);
        int diasFuturos = 0;
        while (diasFuturos < 8) {
            if (esHabil(cursor)) {
                for (int i = 0; i < HORARIOS.size(); i++) {
                    boolean dejarDisponible = (i + diasFuturos) % 3 == 0;
                    if (dejarDisponible) {
                        creados.add(crearTurno(medico, cursor, HORARIOS.get(i), duracion, EstadoTurno.DISPONIBLE, null,
                                EstadoAsistencia.PENDIENTE, ConfirmacionPrevia.SIN_RESPUESTA));
                    } else {
                        Usuario p = pacientes.get(contadorPaciente++ % pacientes.size());
                        ConfirmacionPrevia confirmacion = diasFuturos == 0
                                ? (i % 2 == 0 ? ConfirmacionPrevia.CONFIRMADO : ConfirmacionPrevia.RECHAZADO)
                                : ConfirmacionPrevia.SIN_RESPUESTA;
                        creados.add(crearTurno(medico, cursor, HORARIOS.get(i), duracion, EstadoTurno.RESERVADO, p,
                                EstadoAsistencia.PENDIENTE, confirmacion));
                    }
                }
                diasFuturos++;
            }
            cursor = cursor.plusDays(1);
        }

        return creados;
    }

    private boolean esHabil(LocalDate fecha) {
        return fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private Turno crearTurno(Medico medico, LocalDate fecha, LocalTime horaInicio, int duracionMinutos,
                              EstadoTurno estado, Usuario paciente, EstadoAsistencia estadoAsistencia,
                              ConfirmacionPrevia confirmacionPrevia) {
        Turno t = new Turno();
        t.setMedico(medico);
        t.setFecha(fecha);
        t.setHoraInicio(horaInicio);
        t.setHoraFin(horaInicio.plusMinutes(duracionMinutos));
        t.setEstado(estado);
        t.setPaciente(paciente);
        t.setEstadoAsistencia(estadoAsistencia);
        t.setConfirmacionPrevia(confirmacionPrevia);
        if (paciente != null && estado == EstadoTurno.RESERVADO) {
            t.setMotivoConsulta(MOTIVOS.get((int) (Math.random() * MOTIVOS.size())));
        }
        return turnoRepository.save(t);
    }

    private static final List<String> TEXTOS_EVOLUCION = List.of(
            "Paciente refiere mejoría del cuadro. Continúa con el plan de ejercicios indicado. Se ajusta carga progresivamente.",
            "Primera consulta. Se realiza evaluación funcional completa. Se indica plan de tratamiento de 4 semanas.",
            "Buena evolución del dolor referido. Rango de movimiento casi completo. Se agregan ejercicios de fortalecimiento.",
            "Sin cambios significativos respecto a la sesión anterior. Se mantiene el mismo esquema de trabajo."
    );

    private void cargarEvoluciones(List<Turno> turnos, Usuario secretaria) {
        int i = 0;
        for (Turno t : turnos) {
            if (t.getEstadoAsistencia() == EstadoAsistencia.ATENDIDO && t.getPaciente() != null) {
                EvolucionClinica e = new EvolucionClinica();
                e.setPaciente(t.getPaciente());
                e.setMedico(t.getMedico());
                e.setTurno(t);
                e.setTexto(TEXTOS_EVOLUCION.get(i++ % TEXTOS_EVOLUCION.size()));
                evolucionClinicaRepository.save(e);
            }
        }
    }

    private Usuario kinesiologiaPlanPaciente(List<Usuario> pacientes) {
        return pacientes.get(0);
    }

    private void cargarKinesiologia(Usuario paciente, Medico medicoValidado, Medico medicoPendiente,
                                     Usuario secretaria, List<Turno> turnosMedicoValidado) throws Exception {
        String ordenValidada = guardarPlaceholder("kinesiologia/ordenes-medicas", "orden-demo-1.pdf");
        PlanKinesiologia planValidado = new PlanKinesiologia();
        planValidado.setPaciente(paciente);
        planValidado.setMedico(medicoValidado);
        planValidado.setCantidadSesiones(10);
        planValidado.setSesionesGeneradas(4);
        planValidado.setFechaInicio(LocalDate.now().minusWeeks(2));
        planValidado.setEstado(EstadoPlanKinesiologia.VALIDADO);
        planValidado.setOrdenMedicaPath(ordenValidada);
        planValidado.setOrdenMedicaContentType("application/pdf");
        planValidado.setValidadoPor(secretaria);
        planValidado.setValidadoAt(Instant.now().minus(14, ChronoUnit.DAYS));
        PlanKinesiologiaHorario h1 = new PlanKinesiologiaHorario();
        h1.setPlan(planValidado);
        h1.setDiaSemana(DayOfWeek.MONDAY);
        h1.setHoraInicio(LocalTime.of(15, 0));
        PlanKinesiologiaHorario h2 = new PlanKinesiologiaHorario();
        h2.setPlan(planValidado);
        h2.setDiaSemana(DayOfWeek.WEDNESDAY);
        h2.setHoraInicio(LocalTime.of(15, 0));
        planValidado.getHorarios().add(h1);
        planValidado.getHorarios().add(h2);
        planKinesiologiaRepository.save(planValidado);

        // Vincula un par de turnos ya atendidos del médico como "sesiones ya generadas" del plan.
        turnosMedicoValidado.stream()
                .filter(t -> t.getEstadoAsistencia() == EstadoAsistencia.ATENDIDO && paciente.equals(t.getPaciente()))
                .limit(2)
                .forEach(t -> {
                    t.setPlanKinesiologia(planValidado);
                    turnoRepository.save(t);
                });

        String ordenPendiente = guardarPlaceholder("kinesiologia/ordenes-medicas", "orden-demo-2.pdf");
        PlanKinesiologia planPendiente = new PlanKinesiologia();
        planPendiente.setPaciente(paciente);
        planPendiente.setMedico(medicoPendiente);
        planPendiente.setCantidadSesiones(8);
        planPendiente.setFechaInicio(LocalDate.now().plusDays(3));
        planPendiente.setEstado(EstadoPlanKinesiologia.PENDIENTE_VALIDACION);
        planPendiente.setOrdenMedicaPath(ordenPendiente);
        planPendiente.setOrdenMedicaContentType("application/pdf");
        PlanKinesiologiaHorario h3 = new PlanKinesiologiaHorario();
        h3.setPlan(planPendiente);
        h3.setDiaSemana(DayOfWeek.TUESDAY);
        h3.setHoraInicio(LocalTime.of(16, 0));
        planPendiente.getHorarios().add(h3);
        planKinesiologiaRepository.save(planPendiente);
    }

    private void cargarCaja(Medico medico1, Medico medico2, Usuario secretaria, Usuario contador, List<Turno> turnosMedico1) {
        LocalDate hoy = LocalDate.now();
        List<Turno> reservadosMedico1 = turnosMedico1.stream()
                .filter(t -> t.getEstado() == EstadoTurno.RESERVADO).limit(4).toList();

        MetodoPago[] metodos = MetodoPago.values();
        int i = 0;
        for (Turno t : reservadosMedico1) {
            Ingreso ing = new Ingreso();
            ing.setMedico(medico1);
            ing.setTurno(t);
            ing.setMonto(medico1.getTarifaConsulta() != null ? medico1.getTarifaConsulta() : new BigDecimal("15000"));
            ing.setMetodoPago(metodos[i % metodos.length]);
            ing.setFecha(t.getFecha().isBefore(hoy) || t.getFecha().isEqual(hoy) ? t.getFecha() : hoy);
            ing.setRegistradoPor(secretaria);
            ingresoRepository.save(ing);
            i++;
        }
        // Un ingreso suelto (sin turno) y otro anulado, para mostrar esos dos casos en la UI.
        Ingreso suelto = new Ingreso();
        suelto.setMedico(medico2);
        suelto.setMonto(new BigDecimal("18000"));
        suelto.setMetodoPago(MetodoPago.EFECTIVO);
        suelto.setFecha(hoy.minusDays(2));
        suelto.setRegistradoPor(secretaria);
        ingresoRepository.save(suelto);

        Ingreso anulado = new Ingreso();
        anulado.setMedico(medico1);
        anulado.setMonto(new BigDecimal("15000"));
        anulado.setMetodoPago(MetodoPago.TRANSFERENCIA);
        anulado.setFecha(hoy.minusDays(3));
        anulado.setRegistradoPor(secretaria);
        anulado.setAnulado(true);
        anulado.setMotivoAnulacion("Turno cobrado por error, el paciente había cancelado");
        ingresoRepository.save(anulado);

        Retiro retiro = new Retiro();
        retiro.setMedico(medico1);
        retiro.setMonto(new BigDecimal("15000"));
        retiro.setEntregadoPor(secretaria);
        retiro.setFechaHora(Instant.now().minus(1, ChronoUnit.DAYS));
        retiroRepository.save(retiro);

        List<CategoriaGasto> categorias = categoriaGastoRepository.findAllByOrderByNombreAsc();
        String[] descripciones = {"Factura de luz de octubre", "Compra de camillas descartables", "Sueldo administrativa", "Detergente y guantes"};
        for (int c = 0; c < Math.min(4, categorias.size()); c++) {
            Gasto g = new Gasto();
            g.setCategoria(categorias.get(c));
            g.setMonto(new BigDecimal(8000 + c * 3500));
            g.setFecha(hoy.minusDays(c + 1L));
            g.setDescripcion(descripciones[c % descripciones.length]);
            g.setRegistradoPor(secretaria);
            gastoRepository.save(g);
        }
        Gasto gastoAnulado = new Gasto();
        gastoAnulado.setCategoria(categorias.get(0));
        gastoAnulado.setMonto(new BigDecimal("5000"));
        gastoAnulado.setFecha(hoy.minusDays(5));
        gastoAnulado.setDescripcion("Cargado por error, duplicado");
        gastoAnulado.setRegistradoPor(secretaria);
        gastoAnulado.setAnulado(true);
        gastoAnulado.setMotivoAnulacion("Duplicado con otro gasto ya cargado");
        gastoRepository.save(gastoAnulado);
    }

    private void cargarMensajesYPostulaciones() throws Exception {
        MensajeContacto m1 = new MensajeContacto();
        m1.setNombre("Roberto Alonso");
        m1.setEmail("roberto.alonso@ejemplo.com");
        m1.setTelefono("1155667788");
        m1.setMensaje("Hola, quería consultar disponibilidad de consultorios para alquiler por hora. ¿Tienen turnos libres los martes?");
        m1.setLeido(true);
        mensajeContactoRepository.save(m1);

        MensajeContacto m2 = new MensajeContacto();
        m2.setNombre("Estudio Jurídico Paz & Asoc.");
        m2.setEmail("contacto@pazyasoc.ejemplo.com");
        m2.setTelefono("1144556677");
        m2.setMensaje("Buenas tardes, nos interesa alquilar un consultorio de forma fija dos veces por semana. Aguardo información.");
        m2.setLeido(false);
        mensajeContactoRepository.save(m2);

        MensajeContacto m3 = new MensajeContacto();
        m3.setNombre("Dra. Karina Suárez");
        m3.setEmail("karina.suarez@ejemplo.com");
        m3.setMensaje("¿Los consultorios vienen con camilla incluida? Necesito saber el costo mensual aproximado.");
        m3.setLeido(false);
        mensajeContactoRepository.save(m3);

        String cv1 = guardarPlaceholder("postulaciones/cv", "cv-demo-1.pdf");
        Postulacion p1 = new Postulacion();
        p1.setNombreCompleto("Nicolás Ferreyra");
        p1.setEmail("nicolas.ferreyra@ejemplo.com");
        p1.setTelefono("1166778899");
        p1.setMensaje("Kinesiólogo recibido en la UBA, 3 años de experiencia en rehabilitación deportiva.");
        p1.setCvPath(cv1);
        p1.setCvNombreOriginal("CV_Nicolas_Ferreyra.pdf");
        p1.setCvContentType("application/pdf");
        p1.setCvSize(Files.size(Path.of(cv1)));
        p1.setRevisado(true);
        postulacionRepository.save(p1);

        String cv2 = guardarPlaceholder("postulaciones/cv", "cv-demo-2.pdf");
        Postulacion p2 = new Postulacion();
        p2.setNombreCompleto("Agustina Molina");
        p2.setEmail("agustina.molina@ejemplo.com");
        p2.setTelefono("1177889900");
        p2.setMensaje("Secretaria con experiencia en centros de salud, manejo de agenda y atención al público.");
        p2.setCvPath(cv2);
        p2.setCvNombreOriginal("CV_Agustina_Molina.pdf");
        p2.setCvContentType("application/pdf");
        p2.setCvSize(Files.size(Path.of(cv2)));
        p2.setRevisado(false);
        postulacionRepository.save(p2);
    }

    private void cargarListaEsperaYBaja(Medico medico, List<Usuario> pacientes) {
        ListaEspera le = new ListaEspera();
        le.setPaciente(pacientes.get(6));
        le.setMedico(medico);
        le.setFecha(LocalDate.now().plusDays(3));
        le.setEstado(EstadoListaEspera.ACTIVA);
        listaEsperaRepository.save(le);

        SolicitudBajaCuenta baja = new SolicitudBajaCuenta();
        baja.setUsuario(pacientes.get(7));
        baja.setMotivo("Me mudé de ciudad y no voy a poder seguir viniendo al centro.");
        baja.setEstado(EstadoSolicitudBaja.PENDIENTE);
        solicitudBajaCuentaRepository.save(baja);
    }

    /** PDF mínimo pero válido (abre en cualquier lector) para que "ver CV" / "ver orden médica" no rompan en la demo. */
    private String guardarPlaceholder(String carpeta, String nombreArchivo) throws Exception {
        String contenido = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 300 150]/Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj\n"
                + "4 0 obj<</Length 78>>stream\nBT /F1 14 Tf 20 100 Td (Documento de ejemplo - FKT Lomas demo) Tj ET\nendstream endobj\n"
                + "5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj\n"
                + "trailer<</Root 1 0 R>>\n%%EOF";
        Path directorio = Path.of(uploadsDir, carpeta);
        Files.createDirectories(directorio);
        Path destino = directorio.resolve(nombreArchivo);
        Files.writeString(destino, contenido, StandardCharsets.US_ASCII);
        return destino.toString();
    }
}
