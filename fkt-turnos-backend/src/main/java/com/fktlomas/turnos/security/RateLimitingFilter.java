package com.fktlomas.turnos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Límite de solicitudes por IP para los endpoints públicos más expuestos a abuso
 * (formularios sin autenticación y login, para dificultar fuerza bruta / spam).
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private record Regla(String metodo, String path, int maxIntentos, Duration ventana) {
    }

    private static final List<Regla> REGLAS = List.of(
            new Regla("POST", "/api/auth/login", 15, Duration.ofMinutes(5)),
            new Regla("POST", "/api/auth/registro", 8, Duration.ofMinutes(10)),
            new Regla("POST", "/api/contacto", 5, Duration.ofMinutes(10)),
            new Regla("POST", "/api/postulaciones", 5, Duration.ofMinutes(10))
    );

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {
        for (Regla regla : REGLAS) {
            if (regla.metodo().equals(request.getMethod()) && regla.path().equals(request.getRequestURI())) {
                String clave = regla.path() + "|" + ipDelCliente(request);
                if (!rateLimiterService.permitir(clave, regla.maxIntentos(), regla.ventana())) {
                    response.setStatus(429);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":429,\"mensaje\":\"Demasiados intentos, probá de nuevo en unos minutos\"}");
                    return;
                }
                break;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String ipDelCliente(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
