package com.fktlomas.turnos.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * Verifica el ID token que manda Google Identity Services (ver GoogleAuthButton en el
 * frontend) contra las claves públicas de Google — nunca confía en lo que el cliente dice
 * que contiene el token, lo valida criptográficamente (firma, audiencia, emisor, expiración)
 * antes de leer el email. Sin GOOGLE_CLIENT_ID configurado, {@link #habilitado()} es false y
 * AuthService rechaza el login — mismo criterio "fail-closed" que Mercado Pago/Twilio.
 */
@Slf4j
@Service
public class GoogleTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerifierService(@Value("${app.google.client-id}") String clientId) {
        if (clientId == null || clientId.isBlank()) {
            log.warn("GOOGLE_CLIENT_ID no está configurado — 'Iniciar sesión con Google' queda deshabilitado.");
            this.verifier = null;
        } else {
            this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId.trim()))
                    .build();
        }
    }

    public boolean habilitado() {
        return verifier != null;
    }

    /**
     * @return los datos verificados del token, o {@code null} si no es válido (firma, audiencia
     * o expiración incorrectas) — nunca lanza por un token simplemente inválido, ya que eso es
     * una entrada de usuario normal (alguien reenviando un token viejo, por ejemplo), no un
     * error del servidor.
     */
    public DatosGoogle verificar(String credential) {
        if (!habilitado()) {
            throw new IllegalStateException("El login con Google no está disponible en este momento");
        }
        try {
            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                return null;
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                return null;
            }
            String nombre = (String) payload.get("name");
            return new DatosGoogle(payload.getSubject(), payload.getEmail(), nombre);
        } catch (GeneralSecurityException | java.io.IOException | IllegalArgumentException e) {
            log.warn("No se pudo verificar un ID token de Google: {}", e.getMessage());
            return null;
        }
    }

    public record DatosGoogle(String googleId, String email, String nombre) {
    }
}
