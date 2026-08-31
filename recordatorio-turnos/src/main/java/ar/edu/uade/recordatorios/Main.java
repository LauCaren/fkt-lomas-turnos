package ar.edu.uade.recordatorios;

import ar.edu.uade.recordatorios.config.AppConfig;
import ar.edu.uade.recordatorios.config.DatabaseConfig;
import ar.edu.uade.recordatorios.config.LocalDatabaseConfig;
import ar.edu.uade.recordatorios.config.LocalSchemaInitializer;
import ar.edu.uade.recordatorios.config.LoggingConfig;
import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.notification.EmailNotificador;
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

import javax.sql.DataSource;

/**
 * Entrada headless (para instalar como servicio en Linux, ver install.sh).
 * Usa dos bases: una de SOLO LECTURA al sistema de turnos real (origen), y
 * la propia de la app (local, embebida) donde vive el espejo de pacientes/
 * turnos con el canal preferido. Ver SincronizadorService.
 */
public class Main {

    public static void main(String[] args) {
        LoggingConfig.inicializar();
        AppConfig config = new AppConfig("application.properties");

        DataSource origenDataSource = DatabaseConfig.crearDataSource(
            config.get("origen.db.url"), config.get("origen.db.usuario"), config.get("origen.db.password"));
        DataSource localDataSource = LocalDatabaseConfig.crearDataSource();
        LocalSchemaInitializer.inicializar(localDataSource);

        OrigenRepository origenRepository = new JdbcOrigenRepository(origenDataSource);
        TurnoRepository turnoRepository = new JdbcTurnoRepository(localDataSource);
        RecordatorioRepository recordatorioRepository = new JdbcRecordatorioRepository(localDataSource);
        PacienteRepository pacienteRepository = new JdbcPacienteRepository(localDataSource);
        SincronizadorService sincronizadorService = new SincronizadorService(origenRepository, localDataSource);

        String plantillaMensaje = config.getOrDefault("mensaje.plantilla", PlantillaMensaje.PLANTILLA_POR_DEFECTO);

        NotificadorFactory factory = new NotificadorFactory();
        factory.registrar(CanalNotificacion.EMAIL, new EmailNotificador(
            config.get("smtp.host"), config.get("smtp.usuario"), config.get("smtp.password"), plantillaMensaje));
        factory.registrar(CanalNotificacion.SMS, new SmsNotificador(
            config.get("twilio.accountSid"), config.get("twilio.authToken"), config.get("twilio.numeroSms"), plantillaMensaje));
        factory.registrar(CanalNotificacion.WHATSAPP, new WhatsAppNotificador(
            config.get("twilio.accountSid"), config.get("twilio.authToken"), config.get("twilio.numeroWhatsApp"), plantillaMensaje));

        RecordatorioService recordatorioService = new RecordatorioService(
            turnoRepository, recordatorioRepository, pacienteRepository, factory, sincronizadorService);

        int horasAnticipacion = config.getInt("recordatorio.horasAnticipacion");
        SchedulerService scheduler = new SchedulerService();
        scheduler.iniciar(() -> recordatorioService.procesarRecordatoriosPendientes(horasAnticipacion),
            config.getInt("recordatorio.intervaloMinutos"));

        Runtime.getRuntime().addShutdownHook(new Thread(scheduler::detener));
    }
}
