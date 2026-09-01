package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Tabla de auditoría genérica: quién hizo qué, cuándo, y un snapshot antes/después en JSON.
 * Por ahora solo se usa para {@code tipoEntidad = "TURNO"} (ver TurnoService) — el undo del
 * punto 11 se apoya en esta tabla; extenderla a otras entidades es un paso natural después.
 */
@Entity
@Table(name = "registro_auditoria")
@Getter
@Setter
@NoArgsConstructor
public class RegistroAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "tipo_entidad", nullable = false, length = 50)
    private String tipoEntidad;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Column(nullable = false, length = 40)
    private String operacion;

    @Column(name = "snapshot_antes", columnDefinition = "TEXT")
    private String snapshotAntes;

    @Column(name = "snapshot_despues", columnDefinition = "TEXT")
    private String snapshotDespues;

    @Column(nullable = false)
    private boolean deshecho = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
