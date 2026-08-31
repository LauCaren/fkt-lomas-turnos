package ar.edu.uade.recordatorios.notification;

import ar.edu.uade.recordatorios.model.CanalNotificacion;

import java.util.EnumMap;
import java.util.Map;

public class NotificadorFactory {

    private final Map<CanalNotificacion, Notificador> notificadores = new EnumMap<>(CanalNotificacion.class);

    /** Un canal sin configurar (credenciales incompletas) simplemente no se registra. */
    public void registrar(CanalNotificacion canal, Notificador notificador) {
        if (notificador != null) {
            notificadores.put(canal, notificador);
        }
    }

    public boolean estaConfigurado(CanalNotificacion canal) {
        return notificadores.containsKey(canal);
    }

    public Notificador obtener(CanalNotificacion canal) {
        Notificador notificador = notificadores.get(canal);
        if (notificador == null) {
            throw new IllegalArgumentException("El canal " + canal + " no está configurado");
        }
        return notificador;
    }
}
