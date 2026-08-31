package ar.edu.uade.recordatorios.model;

import java.time.LocalDateTime;

public class Turno {

    private final Long id;
    private final Paciente paciente;
    private final LocalDateTime fechaHora;
    private final String profesional;
    private final String especialidad;
    private final String consultorio;

    public Turno(Long id, Paciente paciente, LocalDateTime fechaHora,
                 String profesional, String especialidad, String consultorio) {
        this.id = id;
        this.paciente = paciente;
        this.fechaHora = fechaHora;
        this.profesional = profesional;
        this.especialidad = especialidad;
        this.consultorio = consultorio;
    }

    public Long getId() { return id; }
    public Paciente getPaciente() { return paciente; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getProfesional() { return profesional; }
    public String getEspecialidad() { return especialidad; }
    public String getConsultorio() { return consultorio; }
}
