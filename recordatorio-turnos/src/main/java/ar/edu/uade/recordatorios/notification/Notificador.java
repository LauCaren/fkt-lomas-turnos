package ar.edu.uade.recordatorios.notification;

import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Turno;

/**
 * Estrategia de envío. Cada canal (email, SMS, WhatsApp) tiene su propia
 * implementación, intercambiable en tiempo de ejecución según el canal
 * preferido del paciente (ver NotificadorFactory).
 */
public interface Notificador {

    void enviar(Turno turno) throws NotificacionException;

    /** Mensaje libre a un paciente, fuera del flujo automático de recordatorios de turno. */
    void enviarMensaje(Paciente paciente, String mensaje) throws NotificacionException;
}
