package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "recordatorios")
@Getter
@Setter
@NoArgsConstructor
public class Recordatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_id", nullable = false, unique = true)
    private Turno turno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacion canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecordatorio estado = EstadoRecordatorio.PENDIENTE;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "error_mensaje", length = 500)
    private String errorMensaje;

    @Column(name = "fecha_envio")
    private Instant fechaEnvio;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Recordatorio(Turno turno, CanalNotificacion canal) {
        this.turno = turno;
        this.canal = canal;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
