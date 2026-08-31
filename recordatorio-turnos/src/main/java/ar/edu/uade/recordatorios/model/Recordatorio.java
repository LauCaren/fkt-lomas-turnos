package ar.edu.uade.recordatorios.model;

import java.time.LocalDateTime;

public class Recordatorio {

    private Long id;
    private final Turno turno;
    private final CanalNotificacion canal;
    private EstadoRecordatorio estado;
    private LocalDateTime fechaEnvio;
    private int intentos;
    private String detalleError;

    public Recordatorio(Turno turno, CanalNotificacion canal) {
        this.turno = turno;
        this.canal = canal;
        this.estado = EstadoRecordatorio.PENDIENTE;
        this.intentos = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Turno getTurno() { return turno; }
    public CanalNotificacion getCanal() { return canal; }
    public EstadoRecordatorio getEstado() { return estado; }
    public void setEstado(EstadoRecordatorio estado) { this.estado = estado; }
    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }
    public int getIntentos() { return intentos; }
    public void incrementarIntentos() { this.intentos++; }
    public void setIntentos(int intentos) { this.intentos = intentos; }
    public String getDetalleError() { return detalleError; }
    public void setDetalleError(String detalleError) { this.detalleError = detalleError; }
}
