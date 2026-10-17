package com.fktlomas.turnos.service;

import tools.jackson.databind.ObjectMapper;
import com.fktlomas.turnos.domain.ConversacionWhatsApp;
import com.fktlomas.turnos.domain.EstadoConversacion;
import com.fktlomas.turnos.domain.Medico;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.EspecialidadDto;
import com.fktlomas.turnos.dto.MedicoDto;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.TurnoDto;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.exception.TurnoNoDisponibleException;
import com.fktlomas.turnos.repository.ConversacionWhatsAppRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.security.RateLimiterService;
import com.fktlomas.turnos.security.TelefonoNormalizador;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Orquesta la conversación de WhatsApp de un paciente: identificarse por DNI, reservar,
 * cancelar o anotarse en lista de espera. Deliberadamente NO depende de nada de historia
 * clínica, caja/pagos ni edición de perfil — si una PR futura le agrega una dependencia así,
 * es la señal de que se está saliendo del alcance pensado para este canal (ver el plan de
 * este feature). Todo el flujo es por menú numerado en texto plano, no lenguaje libre: v1 no
 * usa mensajes interactivos de Twilio (listas/botones) para no depender de aprobar Content
 * Templates — se puede migrar después sin tocar la máquina de estados, solo cómo se arma cada
 * lista y se parsea la respuesta.
 *
 * El teléfono que escribe NUNCA identifica por sí solo a un paciente (ver ConversacionWhatsApp)
 * — toda conversación pasa por ESPERANDO_DNI al menos una vez, y el rate limit de intentos
 * (RateLimiterService, igual que MercadoPagoService para pagos) evita que alguien use el bot
 * para adivinar el DNI de otro paciente y tocar sus turnos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppBotService {

    private static final int MAX_INTENTOS_DNI = 5;
    private static final Duration VENTANA_INTENTOS_DNI = Duration.ofMinutes(15);
    private static final Duration TIMEOUT_INACTIVIDAD = Duration.ofMinutes(15);
    private static final int DIAS_SUGERIDOS = 7;

    private final ConversacionWhatsAppRepository conversacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final EspecialidadService especialidadService;
    private final MedicoService medicoService;
    private final TurnoService turnoService;
    private final ListaEsperaService listaEsperaService;
    private final WhatsAppService whatsAppService;
    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    /**
     * Se busca en cada reserva, no se cachea en @PostConstruct: WhatsAppBotAccountInitializer
     * (un CommandLineRunner) crea este usuario DESPUÉS de que los @PostConstruct de todos los
     * beans ya corrieron, así que cachearlo ahí lo dejaría null la primera vez que arranca la
     * aplicación con la tabla vacía.
     */
    private Usuario actorBot() {
        return usuarioRepository.findByEmail(WhatsAppBotAccountInitializer.EMAIL_BOT)
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el usuario de sistema del bot de WhatsApp (" + WhatsAppBotAccountInitializer.EMAIL_BOT + ")"));
    }

    @Transactional
    public void procesarMensajeEntrante(Map<String, String> parametros) {
        String telefono = TelefonoNormalizador.normalizar(parametros.get("From"));
        String cuerpo = parametros.getOrDefault("Body", "").trim();
        if (telefono == null || telefono.isBlank()) {
            return;
        }

        ConversacionWhatsApp conversacion = conversacionRepository.findByTelefono(telefono)
                .orElseGet(() -> nuevaConversacion(telefono));

        if (Duration.between(conversacion.getUltimaActividad(), Instant.now()).compareTo(TIMEOUT_INACTIVIDAD) > 0) {
            conversacion.reiniciar();
        }

        String cuerpoLower = cuerpo.toLowerCase(Locale.ROOT);
        if ("salir".equals(cuerpoLower)) {
            conversacion.reiniciar();
            enviar(telefono, "Listo, cualquier cosa escribime de nuevo.");
            guardar(conversacion);
            return;
        }
        if ("menu".equals(cuerpoLower) && conversacion.getPaciente() != null) {
            conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
            guardarContexto(conversacion, Contexto.vacio());
            enviar(telefono, textoMenuPrincipal(conversacion.getPaciente()));
            guardar(conversacion);
            return;
        }

        try {
            despachar(conversacion, telefono, cuerpo);
        } catch (RuntimeException e) {
            log.warn("Error procesando WhatsApp de {} en estado {}: {}", telefono, conversacion.getEstado(), e.getMessage());
            enviar(telefono, "Uh, no pudimos hacer eso: " + e.getMessage() + "\nEscribí *menu* para volver al inicio.");
        }
        guardar(conversacion);
    }

    private ConversacionWhatsApp nuevaConversacion(String telefono) {
        ConversacionWhatsApp c = new ConversacionWhatsApp();
        c.setTelefono(telefono);
        return c;
    }

    private void guardar(ConversacionWhatsApp conversacion) {
        conversacion.setUltimaActividad(Instant.now());
        conversacionRepository.save(conversacion);
    }

    private void despachar(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        switch (conversacion.getEstado()) {
            case INICIO -> {
                conversacion.setEstado(EstadoConversacion.ESPERANDO_DNI);
                enviar(telefono, "Hola! Soy el asistente de turnos de FKT Lomas. Para empezar, decime tu número de DNI (solo números).\n\n"
                        + "Al escribirnos aceptás nuestra política de privacidad: fktlomas.com/politica-de-privacidad.html");
            }
            case ESPERANDO_DNI -> manejarEsperandoDni(conversacion, telefono, cuerpo);
            case BLOQUEADO_TEMPORAL -> enviar(telefono, "Todavía no podés reintentar. Escribinos por este mismo WhatsApp más tarde o llamá al consultorio.");
            case REGISTRANDO_NOMBRE -> {
                if (cuerpo.isBlank() || cuerpo.length() > 150) {
                    enviar(telefono, "Decime tu nombre completo (hasta 150 caracteres).");
                    return;
                }
                Contexto ctx = leerContexto(conversacion);
                guardarContexto(conversacion, ctx.conNombreNuevo(cuerpo));
                conversacion.setEstado(EstadoConversacion.REGISTRANDO_OBRA_SOCIAL);
                enviar(telefono, "Gracias. ¿Cuál es tu obra social? Escribí *Particular* si no tenés.");
            }
            case REGISTRANDO_OBRA_SOCIAL -> {
                if (cuerpo.isBlank() || cuerpo.length() > 100) {
                    enviar(telefono, "Decime tu obra social (o *Particular*).");
                    return;
                }
                Contexto ctx = leerContexto(conversacion).conObraSocialNueva(cuerpo);
                guardarContexto(conversacion, ctx);
                iniciarEleccionEspecialidad(conversacion, telefono, EstadoConversacion.ELIGIENDO_ESPECIALIDAD,
                        "Dale, reservemos tu primer turno. Elegí la especialidad:");
            }
            case MENU_PRINCIPAL -> manejarMenuPrincipal(conversacion, telefono, cuerpo);
            case ELIGIENDO_ESPECIALIDAD -> manejarEleccionEspecialidad(conversacion, telefono, cuerpo, EstadoConversacion.ELIGIENDO_MEDICO);
            case ELIGIENDO_MEDICO -> manejarEleccionMedico(conversacion, telefono, cuerpo, EstadoConversacion.ELIGIENDO_FECHA);
            case ELIGIENDO_FECHA -> manejarEleccionFecha(conversacion, telefono, cuerpo);
            case ELIGIENDO_HORARIO -> manejarEleccionHorario(conversacion, telefono, cuerpo);
            case CONFIRMANDO_RESERVA -> manejarConfirmacionReserva(conversacion, telefono, cuerpo);
            case CANCELANDO -> manejarCancelacion(conversacion, telefono, cuerpo);
            case ELIGIENDO_ESPECIALIDAD_LISTA_ESPERA -> manejarEleccionEspecialidad(conversacion, telefono, cuerpo, EstadoConversacion.ELIGIENDO_MEDICO_LISTA_ESPERA);
            case ELIGIENDO_MEDICO_LISTA_ESPERA -> manejarEleccionMedico(conversacion, telefono, cuerpo, EstadoConversacion.ELIGIENDO_FECHA_LISTA_ESPERA);
            case ELIGIENDO_FECHA_LISTA_ESPERA -> manejarFechaListaEspera(conversacion, telefono, cuerpo);
        }
    }

    // ---------- Identificación ----------

    private void manejarEsperandoDni(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        Contexto ctx = leerContexto(conversacion);
        if ("nuevo".equalsIgnoreCase(cuerpo) && ctx.dniPendiente() != null) {
            guardarContexto(conversacion, ctx.conDniNuevo(ctx.dniPendiente()));
            conversacion.setEstado(EstadoConversacion.REGISTRANDO_NOMBRE);
            enviar(telefono, "Dale, registremos tus datos. Decime tu nombre completo.");
            return;
        }
        if (!cuerpo.matches("\\d{7,8}")) {
            enviar(telefono, "Ingresá tu DNI, solo números (7 u 8 dígitos).");
            return;
        }
        if (!rateLimiterService.permitir("whatsapp-dni|" + telefono, MAX_INTENTOS_DNI, VENTANA_INTENTOS_DNI)) {
            conversacion.setEstado(EstadoConversacion.BLOQUEADO_TEMPORAL);
            enviar(telefono, "Demasiados intentos con DNI que no encontramos. Escribinos más tarde o llamá al consultorio.");
            return;
        }
        var pacienteEncontrado = usuarioRepository.findByDni(cuerpo);
        if (pacienteEncontrado.isPresent()) {
            Usuario paciente = pacienteEncontrado.get();
            conversacion.setPaciente(paciente);
            conversacion.setIntentosDni(0);
            conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
            guardarContexto(conversacion, Contexto.vacio());
            enviar(telefono, "Hola, " + primerNombre(paciente.getNombreCompleto()) + "!\n\n" + textoMenuPrincipal(paciente));
            return;
        }
        conversacion.setIntentosDni(conversacion.getIntentosDni() + 1);
        guardarContexto(conversacion, ctx.conDniPendiente(cuerpo));
        if (conversacion.getIntentosDni() >= MAX_INTENTOS_DNI) {
            conversacion.setEstado(EstadoConversacion.BLOQUEADO_TEMPORAL);
            enviar(telefono, "Demasiados intentos con DNI que no encontramos. Escribinos más tarde o llamá al consultorio.");
            return;
        }
        enviar(telefono, "No encontramos ningún paciente con ese DNI. Si sos paciente nuevo, escribí *nuevo* para registrarte, o probá de nuevo con tu DNI.");
    }

    private static String primerNombre(String nombreCompleto) {
        return nombreCompleto == null || nombreCompleto.isBlank() ? "" : nombreCompleto.trim().split("\\s+")[0];
    }

    // ---------- Menú principal ----------

    private String textoMenuPrincipal(Usuario paciente) {
        return "¿Qué querés hacer?\n"
                + "1) Reservar un turno\n"
                + "2) Ver mis próximos turnos\n"
                + "3) Cancelar un turno\n"
                + "4) Anotarme en lista de espera\n\n"
                + "Escribí el número de la opción.";
    }

    private void manejarMenuPrincipal(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        Usuario paciente = conversacion.getPaciente();
        switch (cuerpo.trim()) {
            case "1" -> iniciarEleccionEspecialidad(conversacion, telefono, EstadoConversacion.ELIGIENDO_ESPECIALIDAD, "Elegí la especialidad:");
            case "2" -> {
                List<TurnoDto> proximos = turnosProximosReservados(paciente.getId());
                enviar(telefono, proximos.isEmpty()
                        ? "No tenés turnos reservados próximamente.\n\n" + textoMenuPrincipal(paciente)
                        : "Tus próximos turnos:\n" + listaTurnos(proximos) + "\n\n" + textoMenuPrincipal(paciente));
            }
            case "3" -> {
                List<TurnoDto> proximos = turnosProximosReservados(paciente.getId());
                if (proximos.isEmpty()) {
                    enviar(telefono, "No tenés turnos para cancelar.\n\n" + textoMenuPrincipal(paciente));
                    return;
                }
                guardarContexto(conversacion, Contexto.vacio().conOpcionesIds(proximos.stream().map(TurnoDto::id).toList()));
                conversacion.setEstado(EstadoConversacion.CANCELANDO);
                enviar(telefono, "¿Cuál querés cancelar?\n" + listaTurnos(proximos));
            }
            case "4" -> iniciarEleccionEspecialidad(conversacion, telefono, EstadoConversacion.ELIGIENDO_ESPECIALIDAD_LISTA_ESPERA,
                    "Para anotarte en lista de espera, elegí la especialidad:");
            default -> enviar(telefono, "No entendí esa opción.\n\n" + textoMenuPrincipal(paciente));
        }
    }

    private List<TurnoDto> turnosProximosReservados(Long pacienteId) {
        LocalDate hoy = LocalDate.now();
        return turnoService.misTurnos(pacienteId).stream()
                .filter(t -> "RESERVADO".equals(t.estado()) && !t.fecha().isBefore(hoy))
                .toList();
    }

    private String listaTurnos(List<TurnoDto> turnos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < turnos.size(); i++) {
            TurnoDto t = turnos.get(i);
            sb.append(i + 1).append(") ").append(t.especialidadNombre()).append(" con ").append(t.medicoNombre())
                    .append(" — ").append(t.fecha()).append(" ").append(t.horaInicio()).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    // ---------- Reserva: especialidad / médico / fecha / horario / confirmación ----------

    private void iniciarEleccionEspecialidad(ConversacionWhatsApp conversacion, String telefono, EstadoConversacion estadoEspera, String encabezado) {
        List<EspecialidadDto> especialidades = especialidadService.listar().stream()
                .filter(e -> !e.esKinesiologia())
                .toList();
        if (especialidades.isEmpty()) {
            conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
            enviar(telefono, "Por ahora no hay especialidades disponibles por acá. Escribinos por este WhatsApp y te ayuda una persona.");
            return;
        }
        guardarContexto(conversacion, leerContexto(conversacion).conOpcionesIds(especialidades.stream().map(EspecialidadDto::id).toList()));
        conversacion.setEstado(estadoEspera);
        enviar(telefono, encabezado + "\n" + listaConNumeros(especialidades.stream().map(EspecialidadDto::nombre).toList())
                + "\n\n(Para kinesiología escribinos por este mismo WhatsApp y te ayuda una persona — es un flujo aparte.)");
    }

    private void manejarEleccionEspecialidad(ConversacionWhatsApp conversacion, String telefono, String cuerpo, EstadoConversacion siguiente) {
        Contexto ctx = leerContexto(conversacion);
        Integer n = numeroElegido(cuerpo, ctx.opcionesIds().size());
        if (n == null) {
            enviar(telefono, "Elegí uno de los números de la lista.");
            return;
        }
        Long especialidadId = ctx.opcionesIds().get(n - 1);
        List<MedicoDto> medicos = medicoService.listar(especialidadId);
        if (medicos.isEmpty()) {
            conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
            enviar(telefono, "Esa especialidad no tiene profesionales cargados por ahora.\n\n" + textoMenuPrincipal(conversacion.getPaciente()));
            return;
        }
        guardarContexto(conversacion, ctx.conEspecialidadId(especialidadId).conOpcionesIds(medicos.stream().map(MedicoDto::id).toList()));
        conversacion.setEstado(siguiente);
        enviar(telefono, "Elegí el profesional:\n" + listaConNumeros(medicos.stream().map(MedicoDto::nombreCompleto).toList()));
    }

    private void manejarEleccionMedico(ConversacionWhatsApp conversacion, String telefono, String cuerpo, EstadoConversacion siguiente) {
        Contexto ctx = leerContexto(conversacion);
        Integer n = numeroElegido(cuerpo, ctx.opcionesIds().size());
        if (n == null) {
            enviar(telefono, "Elegí uno de los números de la lista.");
            return;
        }
        Long medicoId = ctx.opcionesIds().get(n - 1);
        List<LocalDate> proximosDias = diasSugeridos();
        guardarContexto(conversacion, ctx.conMedicoId(medicoId).conOpcionesFechas(proximosDias.stream().map(LocalDate::toString).toList()));
        conversacion.setEstado(siguiente);
        enviar(telefono, "¿Qué día te queda bien?\n" + listaFechas(proximosDias) + "\n\nO escribí una fecha como 15/09.");
    }

    private List<LocalDate> diasSugeridos() {
        LocalDate hoy = LocalDate.now();
        List<LocalDate> dias = new ArrayList<>();
        for (int i = 0; i < DIAS_SUGERIDOS; i++) {
            dias.add(hoy.plusDays(i));
        }
        return dias;
    }

    private String listaFechas(List<LocalDate> fechas) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fechas.size(); i++) {
            sb.append(i + 1).append(") ").append(fechas.get(i)).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private void manejarEleccionFecha(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        Contexto ctx = leerContexto(conversacion);
        if ("espera".equalsIgnoreCase(cuerpo.trim()) && ctx.fecha() != null) {
            anotarEnListaEspera(conversacion, telefono, ctx.medicoId(), LocalDate.parse(ctx.fecha()));
            return;
        }
        LocalDate fecha = parsearFecha(cuerpo, ctx.opcionesFechas());
        if (fecha == null) {
            enviar(telefono, "No entendí esa fecha. Elegí un número de la lista o escribí una fecha como 15/09.");
            return;
        }
        List<TurnoDto> disponibles = turnoService.disponiblesDeMedico(ctx.medicoId(), fecha);
        if (disponibles.isEmpty()) {
            List<LocalDate> sugeridas = turnoService.proximaDisponibilidad(ctx.medicoId(), fecha);
            if (sugeridas.isEmpty()) {
                guardarContexto(conversacion, ctx.conFecha(fecha.toString()));
                enviar(telefono, "No encontramos horarios libres para ese profesional en los próximos días. Escribí *espera*"
                        + " para anotarte en lista de espera para el " + fecha + ", o *menu* para volver.");
                return;
            }
            guardarContexto(conversacion, ctx.conFecha(fecha.toString()).conOpcionesFechas(sugeridas.stream().map(LocalDate::toString).toList()));
            enviar(telefono, "No hay lugar el " + fecha + ". Próximas fechas con lugar:\n" + listaFechas(sugeridas)
                    + "\n\nElegí un número, o escribí *espera* para anotarte en lista de espera para el " + fecha + ".");
            return;
        }
        guardarContexto(conversacion, ctx.conFecha(fecha.toString()).conOpcionesIds(disponibles.stream().map(TurnoDto::id).toList()));
        conversacion.setEstado(EstadoConversacion.ELIGIENDO_HORARIO);
        enviar(telefono, "Horarios libres el " + fecha + ":\n" + listaHorarios(disponibles));
    }

    private String listaHorarios(List<TurnoDto> turnos) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < turnos.size(); i++) {
            sb.append(i + 1).append(") ").append(turnos.get(i).horaInicio()).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private LocalDate parsearFecha(String cuerpo, List<String> opcionesFechas) {
        Integer n = numeroElegido(cuerpo, opcionesFechas.size());
        if (n != null) {
            return LocalDate.parse(opcionesFechas.get(n - 1));
        }
        try {
            String[] partes = cuerpo.trim().split("/");
            if (partes.length == 2) {
                MonthDay md = MonthDay.of(Integer.parseInt(partes[1]), Integer.parseInt(partes[0]));
                LocalDate candidato = md.atYear(LocalDate.now().getYear());
                return candidato.isBefore(LocalDate.now()) ? md.atYear(LocalDate.now().getYear() + 1) : candidato;
            }
            if (partes.length == 3) {
                return LocalDate.parse(cuerpo.trim(), DateTimeFormatter.ofPattern("d/M/uuuu"));
            }
        } catch (NumberFormatException | DateTimeException e) {
            return null;
        }
        return null;
    }

    private void manejarEleccionHorario(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        Contexto ctx = leerContexto(conversacion);
        Integer n = numeroElegido(cuerpo, ctx.opcionesIds().size());
        if (n == null) {
            enviar(telefono, "Elegí uno de los números de la lista.");
            return;
        }
        Long turnoId = ctx.opcionesIds().get(n - 1);
        TurnoDto turno = turnoService.disponiblesDeMedico(ctx.medicoId(), LocalDate.parse(ctx.fecha())).stream()
                .filter(t -> t.id().equals(turnoId)).findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Ese horario ya no está disponible, elegí otro"));
        guardarContexto(conversacion, ctx.conTurnoId(turnoId));
        conversacion.setEstado(EstadoConversacion.CONFIRMANDO_RESERVA);
        enviar(telefono, "Confirmás turno de " + turno.especialidadNombre() + " con " + turno.medicoNombre()
                + " el " + turno.fecha() + " a las " + turno.horaInicio() + "?\n\nRespondé *si* o *no*.");
    }

    private void manejarConfirmacionReserva(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        String respuesta = cuerpo.trim().toLowerCase(Locale.ROOT);
        if (!"si".equals(respuesta) && !"sí".equals(respuesta) && !"no".equals(respuesta)) {
            enviar(telefono, "Respondé *si* o *no*.");
            return;
        }
        if ("no".equals(respuesta)) {
            conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
            guardarContexto(conversacion, Contexto.vacio());
            enviar(telefono, "Sin problema, no se confirmó.\n\n" + textoMenuPrincipal(conversacion.getPaciente()));
            return;
        }

        Contexto ctx = leerContexto(conversacion);
        Usuario pacienteActual = conversacion.getPaciente();
        ReservarParaPacienteRequest request = new ReservarParaPacienteRequest(
                pacienteActual != null ? pacienteActual.getDni() : ctx.dniNuevo(),
                pacienteActual != null ? pacienteActual.getNombreCompleto() : ctx.nombreNuevo(),
                telefono,
                pacienteActual != null ? pacienteActual.getObraSocial() : ctx.obraSocialNueva(),
                null,
                null
        );

        TurnoAdminDto reservado;
        try {
            reservado = turnoService.reservarParaPaciente(ctx.turnoId(), request, actorBot());
        } catch (TurnoNoDisponibleException e) {
            // Alguien más reservó ese horario entre que se lo mostramos y que confirmó — no es
            // un error inesperado, así que no se propaga al catch genérico de arriba.
            conversacion.setEstado(EstadoConversacion.ELIGIENDO_FECHA);
            List<LocalDate> proximosDias = diasSugeridos();
            guardarContexto(conversacion, ctx.conOpcionesFechas(proximosDias.stream().map(LocalDate::toString).toList()));
            enviar(telefono, "Justo se ocupó ese horario. Elegí otra fecha:\n" + listaFechas(proximosDias));
            return;
        }

        Usuario pacienteResuelto = usuarioRepository.findById(reservado.pacienteId()).orElseThrow();
        conversacion.setPaciente(pacienteResuelto);
        conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
        guardarContexto(conversacion, Contexto.vacio());

        StringBuilder mensaje = new StringBuilder("Listo! Turno confirmado: " + reservado.especialidadNombre() + " con "
                + reservado.medicoNombre() + " el " + reservado.fecha() + " a las " + reservado.horaInicio() + ".");
        // Aviso de copago IOMA: hoy solo existe en el frontend web (BookingWizard.jsx), así
        // que el bot lo replica acá porque TurnoService.reservarParaPaciente no lo hace.
        Medico medico = medicoService.obtenerEntidad(reservado.medicoId());
        if (medico.isCobraCopagoIoma() && "IOMA".equalsIgnoreCase(pacienteResuelto.getObraSocial())) {
            mensaje.append("\n\nOjo: con IOMA este profesional cobra un copago en el consultorio.");
        }
        mensaje.append("\n\n").append(textoMenuPrincipal(pacienteResuelto));
        enviar(telefono, mensaje.toString());
    }

    // ---------- Cancelación ----------

    private void manejarCancelacion(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        Contexto ctx = leerContexto(conversacion);
        Integer n = numeroElegido(cuerpo, ctx.opcionesIds().size());
        if (n == null) {
            enviar(telefono, "Elegí uno de los números de la lista, o escribí *menu* para volver.");
            return;
        }
        Long turnoId = ctx.opcionesIds().get(n - 1);
        turnoService.cancelar(turnoId, conversacion.getPaciente());
        conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
        guardarContexto(conversacion, Contexto.vacio());
        enviar(telefono, "Listo, cancelamos ese turno.\n\n" + textoMenuPrincipal(conversacion.getPaciente()));
    }

    // ---------- Lista de espera ----------

    private void manejarFechaListaEspera(ConversacionWhatsApp conversacion, String telefono, String cuerpo) {
        Contexto ctx = leerContexto(conversacion);
        LocalDate fecha = parsearFecha(cuerpo, ctx.opcionesFechas());
        if (fecha == null) {
            enviar(telefono, "No entendí esa fecha. Elegí un número de la lista o escribí una fecha como 15/09.");
            return;
        }
        anotarEnListaEspera(conversacion, telefono, ctx.medicoId(), fecha);
    }

    private void anotarEnListaEspera(ConversacionWhatsApp conversacion, String telefono, Long medicoId, LocalDate fecha) {
        listaEsperaService.anotarse(conversacion.getPaciente(), medicoId, fecha);
        conversacion.setEstado(EstadoConversacion.MENU_PRINCIPAL);
        guardarContexto(conversacion, Contexto.vacio());
        enviar(telefono, "Listo, te anotamos en la lista de espera para el " + fecha + ". Te avisamos si se libera un lugar.\n\n"
                + textoMenuPrincipal(conversacion.getPaciente()));
    }

    // ---------- Helpers de texto/listas ----------

    private String listaConNumeros(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(i + 1).append(") ").append(items.get(i)).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private Integer numeroElegido(String cuerpo, int max) {
        try {
            int n = Integer.parseInt(cuerpo.trim());
            return (n >= 1 && n <= max) ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void enviar(String telefono, String texto) {
        try {
            whatsAppService.enviar(telefono, texto);
        } catch (RuntimeException e) {
            // No relanzar: si falla el envío (Twilio caído, credenciales faltantes en dev), el
            // estado de la conversación igual se guarda — mejor que Twilio reintente el webhook
            // y termine mandando el mismo mensaje duplicado.
            log.warn("No se pudo enviar WhatsApp a {}: {}", telefono, e.getMessage());
        }
    }

    // ---------- Contexto de la conversación (serializado como JSON en contexto_json) ----------

    private Contexto leerContexto(ConversacionWhatsApp conversacion) {
        String json = conversacion.getContextoJson();
        if (json == null || json.isBlank()) {
            return Contexto.vacio();
        }
        try {
            return objectMapper.readValue(json, Contexto.class);
        } catch (Exception e) {
            return Contexto.vacio();
        }
    }

    private void guardarContexto(ConversacionWhatsApp conversacion, Contexto contexto) {
        try {
            conversacion.setContextoJson(objectMapper.writeValueAsString(contexto));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el contexto de la conversación", e);
        }
    }

    /**
     * Selecciones en curso de la conversación. opcionesIds/opcionesFechas son genéricos y se
     * reinterpretan según el estado actual (por ejemplo, en ELIGIENDO_MEDICO son ids de médico;
     * en CANCELANDO, ids de turno) — solo hay una lista "activa" por vez, así que no hace falta
     * un campo separado por cada tipo de lista.
     */
    private record Contexto(
            String dniPendiente,
            String dniNuevo,
            String nombreNuevo,
            String obraSocialNueva,
            Long especialidadId,
            Long medicoId,
            String fecha,
            Long turnoId,
            List<Long> opcionesIds,
            List<String> opcionesFechas
    ) {
        static Contexto vacio() {
            return new Contexto(null, null, null, null, null, null, null, null, List.of(), List.of());
        }

        Contexto conDniPendiente(String v) { return new Contexto(v, dniNuevo, nombreNuevo, obraSocialNueva, especialidadId, medicoId, fecha, turnoId, opcionesIds, opcionesFechas); }
        Contexto conDniNuevo(String v) { return new Contexto(dniPendiente, v, nombreNuevo, obraSocialNueva, especialidadId, medicoId, fecha, turnoId, opcionesIds, opcionesFechas); }
        Contexto conNombreNuevo(String v) { return new Contexto(dniPendiente, dniNuevo, v, obraSocialNueva, especialidadId, medicoId, fecha, turnoId, opcionesIds, opcionesFechas); }
        Contexto conObraSocialNueva(String v) { return new Contexto(dniPendiente, dniNuevo, nombreNuevo, v, especialidadId, medicoId, fecha, turnoId, opcionesIds, opcionesFechas); }
        Contexto conEspecialidadId(Long v) { return new Contexto(dniPendiente, dniNuevo, nombreNuevo, obraSocialNueva, v, medicoId, fecha, turnoId, opcionesIds, opcionesFechas); }
        Contexto conMedicoId(Long v) { return new Contexto(dniPendiente, dniNuevo, nombreNuevo, obraSocialNueva, especialidadId, v, fecha, turnoId, opcionesIds, opcionesFechas); }
        Contexto conFecha(String v) { return new Contexto(dniPendiente, dniNuevo, nombreNuevo, obraSocialNueva, especialidadId, medicoId, v, turnoId, opcionesIds, opcionesFechas); }
        Contexto conTurnoId(Long v) { return new Contexto(dniPendiente, dniNuevo, nombreNuevo, obraSocialNueva, especialidadId, medicoId, fecha, v, opcionesIds, opcionesFechas); }
        Contexto conOpcionesIds(List<Long> v) { return new Contexto(dniPendiente, dniNuevo, nombreNuevo, obraSocialNueva, especialidadId, medicoId, fecha, turnoId, v, opcionesFechas); }
        Contexto conOpcionesFechas(List<String> v) { return new Contexto(dniPendiente, dniNuevo, nombreNuevo, obraSocialNueva, especialidadId, medicoId, fecha, turnoId, opcionesIds, v); }
    }
}
