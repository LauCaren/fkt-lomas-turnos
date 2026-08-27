package com.fktlomas.turnos.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Límite simple por ventana deslizante, en memoria. Alcanza para el volumen actual
 * de un centro médico chico; si esto se despliega con varias instancias detrás de un
 * balanceador, hay que reemplazarlo por algo compartido (Redis, etc.).
 */
@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Deque<Instant>> intentosPorClave = new ConcurrentHashMap<>();

    public boolean permitir(String clave, int maxIntentos, Duration ventana) {
        Instant ahora = Instant.now();
        Instant limite = ahora.minus(ventana);
        Deque<Instant> intentos = intentosPorClave.computeIfAbsent(clave, k -> new ConcurrentLinkedDeque<>());

        synchronized (intentos) {
            while (!intentos.isEmpty() && intentos.peekFirst().isBefore(limite)) {
                intentos.pollFirst();
            }
            if (intentos.size() >= maxIntentos) {
                return false;
            }
            intentos.addLast(ahora);
            return true;
        }
    }
}
