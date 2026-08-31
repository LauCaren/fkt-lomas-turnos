package ar.edu.uade.recordatorios.notification;

public class NotificacionException extends Exception {
    public NotificacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
