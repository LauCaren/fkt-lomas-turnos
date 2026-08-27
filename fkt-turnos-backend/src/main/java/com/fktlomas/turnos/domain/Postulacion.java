package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "postulaciones")
@Getter
@Setter
@NoArgsConstructor
public class Postulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(length = 30)
    private String telefono;

    @Column(length = 2000)
    private String mensaje;

    @Column(name = "cv_path", nullable = false)
    private String cvPath;

    @Column(name = "cv_nombre_original", nullable = false)
    private String cvNombreOriginal;

    @Column(name = "cv_content_type", nullable = false, length = 100)
    private String cvContentType;

    @Column(name = "cv_size", nullable = false)
    private long cvSize;

    @Column(nullable = false)
    private boolean revisado = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }
}
