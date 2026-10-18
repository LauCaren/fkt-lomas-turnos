package com.fktlomas.turnos.security;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
public class JwtService {

    /**
     * Tiene que coincidir exactamente con el default de app.jwt.secret en application.yml.
     * Es un valor público (está en el código fuente): si alguna vez queda activo en un
     * despliegue real, cualquiera podría firmar tokens válidos, incluso de admin. Por eso
     * nunca se usa para firmar — ver el chequeo en el constructor.
     */
    private static final String SECRETO_POR_DEFECTO = "changeme-dev-secret-change-in-prod-please-0123456789";

    private final SecretKey key;
    private final long expirationMinutes;
    private final long expirationMinutesStaff;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                       @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
                       @Value("${app.jwt.expiration-minutes-staff}") long expirationMinutesStaff) {
        if (SECRETO_POR_DEFECTO.equals(secret)) {
            log.warn("JWT_SECRET no está configurado — usando una clave aleatoria generada para esta ejecución. "
                    + "Las sesiones no van a sobrevivir un reinicio del backend. Configurá la variable de entorno "
                    + "JWT_SECRET con un valor propio y secreto antes de desplegar a producción.");
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS384);
        } else {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
        this.expirationMinutes = expirationMinutes;
        this.expirationMinutesStaff = expirationMinutesStaff;
    }

    /**
     * Pacientes: sesión larga (7 días por defecto) para no pedirle la contraseña a cada rato a
     * alguien que entra una vez por semana a ver su turno. Personal (médicos, secretaría, admin,
     * contador): una jornada (12 h por defecto), porque esas cuentas ven historias clínicas y
     * caja de todo el centro — si alguien se lleva el token de una PC de la oficina, deja de
     * servir al día siguiente. Ver app.jwt.* en application.yml.
     */
    public String generarToken(Usuario usuario) {
        Instant now = Instant.now();
        long minutos = usuario.getRole() == Role.PACIENTE ? expirationMinutes : expirationMinutesStaff;
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("uid", usuario.getId())
                .claim("role", usuario.getRole().name())
                .claim("nombre", usuario.getNombreCompleto())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(minutos, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extraerEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Instant extraerFechaEmision(String token) {
        return parseClaims(token).getIssuedAt().toInstant();
    }

    public boolean esValido(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
