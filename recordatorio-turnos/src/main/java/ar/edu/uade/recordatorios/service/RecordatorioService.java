package ar.edu.uade.recordatorios.service;

import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.EstadoRecordatorio;
import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Recordatorio;
import ar.edu.uade.recordatorios.model.Turno;
import ar.edu.uade.recordatorios.notification.Notificador;
import ar.edu.uade.recordatorios.notification.NotificacionException;
import ar.edu.uade.recordatorios.notification.NotificadorFactory;
import ar.edu.uade.recordatorios.repository.PacienteRepository;
import ar.edu.uade.recordatorios.repository.RecordatorioRepository;
import ar.edu.uade.recordatorios.repository.TurnoRepository;
import ar.edu.uade.recordatorios.sync.EstadoSincronizacion;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RecordatorioService {

    private static final Logger log = Logger.getLogger(RecordatorioService.class.getName());
    private static final int MAX_INTENTOS = 3;

    private final TurnoRepository turnoRepository;
    private final RecordatorioRepository recordatorioRepository;
    private final PacienteRepository pacienteRepository;
    private final NotificadorFactory notificadorFactory;
    private final SincronizadorService sincronizadorService;

    public RecordatorioService(TurnoRepository turnoRepository,
                                RecordatorioRepository recordatorioRepository,
                                PacienteRepository pacienteRepository,
                                NotificadorFactory notificadorFactory,
                                SincronizadorService sincronizadorService) {
        this.turnoRepository = turnoRepository;
        this.recordatorioRepository = recordatorioRepository;
        this.pacienteRepository = pacienteRepository;
        this.notificadorFactory = notificadorFactory;
        this.sincronizadorService = sincronizadorService;
    }

    /**
     * Primero sincroniza pacientes/turnos desde el sistema de origen hacia
     * la base local (ver SincronizadorService), y con esos datos ya al día
     * busca los turnos dentro de la ventana de anticipación configurada y
     * dispara el recordatorio por el canal preferido de cada paciente. Este
     * es el método que llama el Scheduler periódicamente (y el botón
     * "Chequear ahora" de la GUI).
     *
     * Un mismo turno puede entrar acá en varias corridas seguidas mientras
     * esté dentro de la ventana y no se haya podido enviar todavía: por eso
     * reusa el Recordatorio existente en vez de crear uno nuevo cada vez
     * (si no, cada chequeo generaría un envío/registro duplicado).
     */
    public void procesarRecordatoriosPendientes(int horasDeAnticipacion) {
        sincronizadorService.sincronizar();

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime limite = ahora.plusHours(horasDeAnticipacion);

        List<Turno> turnos = turnoRepository.buscarTurnosProximos(ahora, limite);
        log.info(() -> "Turnos encontrados para recordar: " + turnos.size());

        for (Turno turno : turnos) {
            Recordatorio recordatorio = recordatorioRepository.buscarPorTurno(turno.getId())
                .orElseGet(() -> recordatorioRepository.guardar(
                    new Recordatorio(turno, turno.getPaciente().getCanalPreferido())));
            intentarEnvio(recordatorio);
        }
    }

    /** Reintento manual desde la GUI para un recordatorio puntual (pendiente o fallido). */
    public void reintentarRecordatorio(Long recordatorioId) {
        Recordatorio recordatorio = recordatorioRepository.buscarPorId(recordatorioId)
            .orElseThrow(() -> new IllegalArgumentException("No existe el recordatorio " + recordatorioId));
        intentarEnvio(recordatorio);
    }

    /** Para el panel de monitoreo de la GUI. */
    public List<Recordatorio> listarUltimos(int limite) {
        return recordatorioRepository.listarUltimos(limite);
    }

    /** Para la pestaña de Pacientes de la GUI. */
    public List<Paciente> listarPacientes() {
        return pacienteRepository.listarTodos();
    }

    public void actualizarCanalPreferido(Long pacienteId, CanalNotificacion canal) {
        pacienteRepository.actualizarCanalPreferido(pacienteId, canal);
    }

    /** Para mostrar en la GUI cuándo sincronizó por última vez con el sistema de turnos y con qué resultado. */
    public EstadoSincronizacion obtenerEstadoSincronizacion() {
        return sincronizadorService.obtenerUltimoEstado();
    }

    /**
     * Mensaje libre a un paciente puntual (ej. avisarle algo fuera del
     * recordatorio automático de turno). No queda registrado en la tabla
     * de recordatorios: esa tabla está atada a un turno, y esto no lo está.
     */
    public void enviarMensajeManual(Paciente paciente, CanalNotificacion canal, String mensaje) throws NotificacionException {
        Notificador notificador = notificadorFactory.obtener(canal);
        notificador.enviarMensaje(paciente, mensaje);
    }

    private void intentarEnvio(Recordatorio recordatorio) {
        Turno turno = recordatorio.getTurno();
        try {
            Notificador notificador = notificadorFactory.obtener(recordatorio.getCanal());
            notificador.enviar(turno);

            recordatorio.setEstado(EstadoRecordatorio.ENVIADO);
            recordatorio.setFechaEnvio(LocalDateTime.now());
            recordatorioRepository.actualizarEstado(recordatorio);

        } catch (Exception e) {
            // Incluye tanto fallos de envío (NotificacionException) como un canal
            // sin configurar (IllegalArgumentException de la factory): en ambos
            // casos se registra el error y se sigue con el resto de los turnos,
            // en vez de abortar todo el proceso del scheduler.
            recordatorio.incrementarIntentos();
            recordatorio.setDetalleError(e.getMessage());
            recordatorio.setEstado(recordatorio.getIntentos() >= MAX_INTENTOS
                ? EstadoRecordatorio.FALLIDO
                : EstadoRecordatorio.PENDIENTE);
            recordatorioRepository.actualizarEstado(recordatorio);

            log.log(Level.WARNING, "Fallo al enviar recordatorio del turno " + turno.getId(), e);
        }
    }
}
