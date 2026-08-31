package ar.edu.uade.recordatorios.gui;

import ar.edu.uade.recordatorios.config.ConfigStore;
import ar.edu.uade.recordatorios.config.DatabaseConfig;
import ar.edu.uade.recordatorios.config.InstanceLock;
import ar.edu.uade.recordatorios.config.LocalDatabaseConfig;
import ar.edu.uade.recordatorios.config.LocalSchemaInitializer;
import ar.edu.uade.recordatorios.config.LoggingConfig;
import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.EstadoRecordatorio;
import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.notification.EmailNotificador;
import ar.edu.uade.recordatorios.notification.NotificacionException;
import ar.edu.uade.recordatorios.notification.Notificador;
import ar.edu.uade.recordatorios.notification.NotificadorFactory;
import ar.edu.uade.recordatorios.notification.PlantillaMensaje;
import ar.edu.uade.recordatorios.notification.SmsNotificador;
import ar.edu.uade.recordatorios.notification.WhatsAppNotificador;
import ar.edu.uade.recordatorios.repository.OrigenRepository;
import ar.edu.uade.recordatorios.repository.PacienteRepository;
import ar.edu.uade.recordatorios.repository.RecordatorioRepository;
import ar.edu.uade.recordatorios.repository.TurnoRepository;
import ar.edu.uade.recordatorios.repository.impl.JdbcOrigenRepository;
import ar.edu.uade.recordatorios.repository.impl.JdbcPacienteRepository;
import ar.edu.uade.recordatorios.repository.impl.JdbcRecordatorioRepository;
import ar.edu.uade.recordatorios.repository.impl.JdbcTurnoRepository;
import ar.edu.uade.recordatorios.service.RecordatorioService;
import ar.edu.uade.recordatorios.service.SchedulerService;
import ar.edu.uade.recordatorios.service.SincronizadorService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

import javax.sql.DataSource;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Punto de entrada de la app de escritorio para FKT Lomas: arranca la GUI y,
 * si hay configuración guardada y completa, el scheduler de recordatorios en
 * segundo plano. A diferencia de Main (headless, para instalaciones tipo
 * servidor), acá la configuración puede estar incompleta al primer arranque:
 * el usuario la completa desde la pestaña "Configuración" y el servicio
 * arranca recién ahí.
 */
public class RecordatorioApp extends Application {

    private static final Logger log = Logger.getLogger(RecordatorioApp.class.getName());

    private MainWindow mainWindow;
    private SchedulerService scheduler;
    private TrayIconManager trayIconManager;
    private int ultimoConteoFallidos;

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);

        mainWindow = new MainWindow(this);
        mainWindow.mostrar(stage);

        Properties props = ConfigStore.cargar();
        mainWindow.cargarConfiguracion(props);
        aplicarConfiguracion(props);

        trayIconManager = new TrayIconManager(stage, this::salir);
        trayIconManager.instalar();
    }

    /** Llamado por la pestaña de Configuración (o la de Mensaje) al tocar "Guardar". */
    void guardarYReiniciar(Properties props) {
        ConfigStore.guardar(props);
        aplicarConfiguracion(props);
    }

    /** Envío puntual desde el botón "Enviar mensaje de prueba" de Configuración, con los datos tal cual están en el formulario (guardados o no). */
    void probarEnvio(CanalNotificacion canal, String destino, Properties props) throws NotificacionException {
        Notificador notificador = crearNotificador(canal, props);
        if (notificador == null) {
            throw new IllegalStateException("Completá las credenciales del canal " + canal + " antes de probarlo.");
        }
        Paciente pacientePrueba = new Paciente(0L, "Prueba", destino, destino, canal);
        notificador.enviarMensaje(pacientePrueba,
            "Este es un mensaje de prueba de Recordatorio de turnos - FKT Lomas. "
            + "Si lo recibiste, ¡la configuración funciona correctamente!");
    }

    private void aplicarConfiguracion(Properties props) {
        detenerServicio();

        if (!esConfiguracionMinimaCompleta(props)) {
            mainWindow.setServicio(null);
            mainWindow.mostrarServicioActivo(false, "Completá al menos la base de datos y el email antes de iniciar.");
            return;
        }

        try {
            RecordatorioService servicio = construirServicio(props);
            mainWindow.setServicio(servicio);
            ultimoConteoFallidos = 0;

            int horas = Integer.parseInt(props.getProperty("recordatorio.horasAnticipacion"));
            int intervalo = Integer.parseInt(props.getProperty("recordatorio.intervaloMinutos"));
            scheduler = new SchedulerService();
            scheduler.iniciar(() -> {
                servicio.procesarRecordatoriosPendientes(horas);
                revisarFallidosYNotificar(servicio);
            }, intervalo);

            mainWindow.mostrarServicioActivo(true, null);
        } catch (Exception e) {
            log.log(Level.SEVERE, "No se pudo iniciar el servicio de recordatorios", e);
            mainWindow.setServicio(null);
            mainWindow.mostrarServicioActivo(false, causaRaiz(e));
        }
    }

    /** Avisa con un globo de la bandeja si aparecieron recordatorios FALLIDO nuevos desde el último chequeo. */
    private void revisarFallidosYNotificar(RecordatorioService servicio) {
        try {
            long fallidos = servicio.listarUltimos(500).stream()
                .filter(r -> r.getEstado() == EstadoRecordatorio.FALLIDO)
                .count();
            if (fallidos > ultimoConteoFallidos && trayIconManager != null) {
                String mensaje = "Hay " + fallidos + " recordatorio(s) que no se pudieron enviar. Abrí la app para revisarlos.";
                SwingUtilities.invokeLater(() -> trayIconManager.mostrarNotificacion("Recordatorio de turnos", mensaje));
            }
            ultimoConteoFallidos = (int) fallidos;
        } catch (Exception e) {
            log.log(Level.WARNING, "No se pudo chequear si hay recordatorios fallidos", e);
        }
    }

    private RecordatorioService construirServicio(Properties props) {
        DataSource origenDataSource = DatabaseConfig.crearDataSource(
            props.getProperty("origen.db.url"), props.getProperty("origen.db.usuario"), props.getProperty("origen.db.password"));
        DataSource localDataSource = LocalDatabaseConfig.crearDataSource();
        LocalSchemaInitializer.inicializar(localDataSource);

        OrigenRepository origenRepository = new JdbcOrigenRepository(origenDataSource);
        TurnoRepository turnoRepository = new JdbcTurnoRepository(localDataSource);
        RecordatorioRepository recordatorioRepository = new JdbcRecordatorioRepository(localDataSource);
        PacienteRepository pacienteRepository = new JdbcPacienteRepository(localDataSource);
        SincronizadorService sincronizadorService = new SincronizadorService(origenRepository, localDataSource);

        NotificadorFactory factory = new NotificadorFactory();
        for (CanalNotificacion canal : CanalNotificacion.values()) {
            try {
                Notificador notificador = crearNotificador(canal, props);
                if (notificador != null) {
                    factory.registrar(canal, notificador);
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "No se pudo configurar el canal " + canal + ": " + e.getMessage());
            }
        }

        return new RecordatorioService(
            turnoRepository, recordatorioRepository, pacienteRepository, factory, sincronizadorService);
    }

    /** null si al canal le faltan credenciales — no todos los canales son obligatorios (Twilio es opcional). */
    private Notificador crearNotificador(CanalNotificacion canal, Properties props) {
        String plantilla = props.getProperty("mensaje.plantilla", PlantillaMensaje.PLANTILLA_POR_DEFECTO);

        return switch (canal) {
            case EMAIL -> {
                if (!noVacio(props, "smtp.host") || !noVacio(props, "smtp.usuario") || !noVacio(props, "smtp.password")) {
                    yield null;
                }
                yield new EmailNotificador(
                    props.getProperty("smtp.host"), props.getProperty("smtp.usuario"),
                    props.getProperty("smtp.password"), plantilla);
            }
            case SMS -> {
                if (!credencialesTwilioCompletas(props) || !noVacio(props, "twilio.numeroSms")) {
                    yield null;
                }
                yield new SmsNotificador(
                    props.getProperty("twilio.accountSid"), props.getProperty("twilio.authToken"),
                    props.getProperty("twilio.numeroSms"), plantilla);
            }
            case WHATSAPP -> {
                if (!credencialesTwilioCompletas(props) || !noVacio(props, "twilio.numeroWhatsApp")) {
                    yield null;
                }
                yield new WhatsAppNotificador(
                    props.getProperty("twilio.accountSid"), props.getProperty("twilio.authToken"),
                    props.getProperty("twilio.numeroWhatsApp"), plantilla);
            }
        };
    }

    private boolean credencialesTwilioCompletas(Properties props) {
        return noVacio(props, "twilio.accountSid") && noVacio(props, "twilio.authToken");
    }

    private boolean esConfiguracionMinimaCompleta(Properties props) {
        return noVacio(props, "origen.db.url") && noVacio(props, "origen.db.usuario")
            && noVacio(props, "smtp.host") && noVacio(props, "smtp.usuario") && noVacio(props, "smtp.password")
            && noVacio(props, "recordatorio.horasAnticipacion") && noVacio(props, "recordatorio.intervaloMinutos");
    }

    private boolean noVacio(Properties props, String clave) {
        String valor = props.getProperty(clave);
        return valor != null && !valor.isBlank();
    }

    private void detenerServicio() {
        if (scheduler != null) {
            scheduler.detener();
            scheduler = null;
        }
    }

    private String causaRaiz(Throwable t) {
        Throwable actual = t;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        return actual.getMessage() != null ? actual.getMessage() : actual.toString();
    }

    private void salir() {
        detenerServicio();
        if (trayIconManager != null) {
            trayIconManager.remover();
        }
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        LoggingConfig.inicializar();

        if (!InstanceLock.adquirir()) {
            JOptionPane.showMessageDialog(null,
                "Recordatorio de turnos ya está corriendo — fijate en la bandeja del sistema.",
                "FKT Lomas", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        launch(args);
    }
}
