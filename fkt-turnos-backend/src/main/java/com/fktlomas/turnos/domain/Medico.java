package com.fktlomas.turnos.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "medicos")
@Getter
@Setter
@NoArgsConstructor
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 150)
    private String nombreCompleto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(length = 50)
    private String matricula;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "duracion_turno_minutos", nullable = false)
    private int duracionTurnoMinutos = 30;

    @Column(name = "anos_experiencia")
    private Integer anosExperiencia;

    @Column(name = "foto_path")
    private String fotoPath;

    @Column(name = "foto_content_type", length = 100)
    private String fotoContentType;

    @Column(name = "titulo_imagen_path")
    private String tituloImagenPath;

    @Column(name = "titulo_imagen_content_type", length = 100)
    private String tituloImagenContentType;
}
