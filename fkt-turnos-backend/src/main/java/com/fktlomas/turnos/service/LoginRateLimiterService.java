package com.fktlomas.turnos.service;

import com.fktlomas.turnos.exception.DemasiadosIntentosException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Protege el login (y el segundo factor MFA) contra fuerza bruta: bloquea por
 * un rato al email que acumule demasiados intentos fallidos seguidos.
 * En memoria únicamente — alcanza para una sola instancia como esta; si algún
 * día corre más de un proceso del backend a la vez, esto habría que moverlo a
 * un store compartido (Redis) para que el límite sea real entre instancias.
 */
@Service
public class LoginRateLimiterService {

    private static final int MAX_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofMinutes(15);
    private static final Duration BLOQUEO = Duration.ofMinutes(15);

    private record Estado(int intentos, Instant primerIntento, Instant bloqueadoHasta) {
        boolean expirado(Instant ahora) {
            return (bloqueadoHasta == null || bloqueadoHasta.isBefore(ahora))
                    && Duration.between(primerIntento, ahora).compareTo(VENTANA) > 0;
        }
    }

    private final Map<String, Estado> porEmail = new ConcurrentHashMap<>();

    public void verificarNoBloqueado(String email) {
        Estado estado = porEmail.get(clave(email));
        if (estado != null && estado.bloqueadoHasta() != null && Instant.now().isBefore(estado.bloqueadoHasta())) {
            long minutos = Math.max(1, Duration.between(Instant.now(), estado.bloqueadoHasta()).toMinutes());
            throw new DemasiadosIntentosException(
                    "Demasiados intentos fallidos. Probá de nuevo en " + minutos + (minutos == 1 ? " minuto." : " minutos."));
        }
    }

    public void registrarFallo(String email) {
        porEmail.compute(clave(email), (k, actual) -> {
            Instant ahora = Instant.now();
            if (actual == null || Duration.between(actual.primerIntento(), ahora).compareTo(VENTANA) > 0) {
                return new Estado(1, ahora, null);
            }
            int nuevos = actual.intentos() + 1;
            Instant bloqueadoHasta = nuevos >= MAX_INTENTOS ? ahora.plus(BLOQUEO) : null;
            return new Estado(nuevos, actual.primerIntento(), bloqueadoHasta);
        });
    }

    public void registrarExito(String email) {
        porEmail.remove(clave(email));
    }

    @Scheduled(fixedRate = 60 * 60 * 1000)
    void limpiarVencidos() {
        Instant ahora = Instant.now();
        porEmail.entrySet().removeIf(e -> e.getValue().expirado(ahora));
    }

    private String clave(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
