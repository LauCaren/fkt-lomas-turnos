package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Fila única (id fijo en 1): a dónde se manda el Excel mensual de caja, configurable por el admin. */
@Entity
@Table(name = "configuracion_caja")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionCaja {

    @Id
    private Short id = 1;

    @Column(name = "email_reporte_mensual", length = 180)
    private String emailReporteMensual;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
