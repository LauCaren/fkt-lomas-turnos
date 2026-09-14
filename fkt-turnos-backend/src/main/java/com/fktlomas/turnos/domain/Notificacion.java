package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Notificación dentro de la app (campanita en el header, ver NotificacionesBell.jsx) — para que
 * un paciente (o, a futuro, staff) vea qué pasó con sus turnos sin depender de revisar el email
 * o WhatsApp, que además pueden fallar en silencio (ver CanalFallbackService). `turnoId` es un
 * Long suelto, no una relación @ManyToOne al turno: el turno puede reprogramarse o el registro
 * purgarse después, y la notificación tiene que seguir mostrando su mensaje igual — la relación
 * es solo informativa, para el deep-link "Ver turno" cuando el turno todavía existe.
 */
@Entity
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(name = "turno_id")
    private Long turnoId;

    @Column(nullable = false)
    private boolean leida = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Notificacion(Usuario usuario, TipoNotificacion tipo, String mensaje, Long turnoId) {
        this.usuario = usuario;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.turnoId = turnoId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
