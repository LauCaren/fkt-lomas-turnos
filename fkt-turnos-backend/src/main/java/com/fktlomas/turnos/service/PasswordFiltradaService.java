package com.fktlomas.turnos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Chequea una contraseña contra la API "Pwned Passwords" de HaveIBeenPwned (k-anonymity: solo
 * se manda el prefijo de 5 caracteres del hash SHA-1, nunca la contraseña ni el hash completo —
 * ver https://haveibeenpwned.com/API/v3#SearchingPwnedPasswordsByRange). No requiere API key.
 *
 * Falla "abierto" a propósito: si el servicio no responde (caído, sin red, timeout), se deja
 * pasar la contraseña sin este chequeo adicional en vez de bloquear el registro/cambio — es una
 * capa extra sobre Patrones.PASSWORD_FUERTE, no la única defensa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordFiltradaService {

    private static final String API_URL = "https://api.pwnedpasswords.com/range/";
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final HttpClient httpClient;

    @Value("${app.seguridad.chequeo-password-filtrada}")
    private boolean habilitado;

    public void verificar(String password) {
        if (!habilitado) {
            return;
        }
        String sha1 = sha1Hex(password);
        String prefijo = sha1.substring(0, 5);
        String sufijo = sha1.substring(5);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + prefijo))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("HaveIBeenPwned respondió {} — se omite el chequeo de contraseña filtrada para este pedido", response.statusCode());
                return;
            }
            boolean filtrada = response.body().lines().anyMatch(linea -> linea.regionMatches(true, 0, sufijo, 0, sufijo.length()));
            if (filtrada) {
                throw new IllegalArgumentException("Esta contraseña apareció en filtraciones de datos conocidas — elegí una distinta");
            }
        } catch (IOException e) {
            log.warn("No se pudo consultar HaveIBeenPwned, se omite el chequeo de contraseña filtrada para este pedido", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Chequeo de HaveIBeenPwned interrumpido, se omite para este pedido");
        }
    }

    private static String sha1Hex(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).toUpperCase(Locale.ROOT);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 no disponible en esta JVM", e);
        }
    }
}
