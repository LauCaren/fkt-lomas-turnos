package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.OffsetDateTime;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    /** Email sintético asignado a pacientes cargados por teléfono sin email real (ver TurnoService.crearPacienteTelefonico). */
    public static final String DOMINIO_EMAIL_SINTETICO = "@sin-email.fktlomas.local";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 10)
    private String dni;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Enumerated(EnumType.STRING)
    private Genero genero;

    @Column(name = "obra_social", length = 100)
    private String obraSocial;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "acepta_politica_privacidad", nullable = false)
    private boolean aceptaPoliticaPrivacidad;

    /** Ver {@link OrigenConsentimiento} — nullable a propósito, ver el comentario de la migración V26. */
    @Enumerated(EnumType.STRING)
    @Column(name = "origen_consentimiento", length = 20)
    private OrigenConsentimiento origenConsentimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Cuándo se fijó la contraseña actual — un JWT emitido antes de esto ya no es válido (ver JwtAuthFilter). */
    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @Column(name = "reset_token", length = 64)
    private String resetToken;

    @Column(name = "reset_token_expira")
    private OffsetDateTime resetTokenExpira;

    @Convert(converter = com.fktlomas.turnos.security.MfaSecretConverter.class)
    @Column(name = "mfa_secret", length = 255)
    private String mfaSecret;

    @Column(name = "mfa_habilitado", nullable = false)
    private boolean mfaHabilitado;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_notificacion_preferido", nullable = false, length = 20)
    private CanalNotificacion canalNotificacionPreferido = CanalNotificacion.EMAIL;

    /** Cuenta de staff (MEDICO/CONTADOR/SECRETARIA/ADMIN) desactivada por un admin — ver AppUserDetails.isEnabled(). No aplica a pacientes, que nunca se desactivan por esta vía. */
    @Column(nullable = false)
    private boolean activo = true;

    /** Igual que passwordChangedAt pero para "cerrar todas las sesiones" sin obligar a cambiar la contraseña — ver JwtAuthFilter. */
    @Column(name = "sessions_revoked_at")
    private Instant sessionsRevokedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (passwordChangedAt == null) {
            passwordChangedAt = Instant.now();
        }
        normalizarEmail();
    }

    /**
     * El email es único a nivel de columna, pero esa restricción es sensible a
     * mayúsculas/minúsculas — sin esto, "Foo@Bar.com" y "foo@bar.com" pasarían
     * como cuentas distintas. Se normaliza acá, en el único punto por el que
     * pasa cualquier alta o cambio de email, sin importar desde qué service se
     * llame (registro, "mi cuenta", alta telefónica del admin, bootstrap).
     */
    @PreUpdate
    void normalizarEmail() {
        if (email != null) {
            email = email.trim().toLowerCase();
        }
    }

    public boolean tieneEmailReal() {
        return email != null && !email.endsWith(DOMINIO_EMAIL_SINTETICO);
    }

    public boolean tieneTelefono() {
        return telefono != null && !telefono.isBlank();
    }
}
