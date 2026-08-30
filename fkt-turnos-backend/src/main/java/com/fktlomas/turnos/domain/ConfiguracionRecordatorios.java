package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Fila única (id fijo en 1): configuración editable desde el panel de admin. */
@Entity
@Table(name = "configuracion_recordatorios")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionRecordatorios {

    @Id
    private Short id = 1;

    @Column(name = "plantilla_mensaje", nullable = false, columnDefinition = "TEXT")
    private String plantillaMensaje;

    @Column(name = "horas_anticipacion", nullable = false)
    private int horasAnticipacion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
