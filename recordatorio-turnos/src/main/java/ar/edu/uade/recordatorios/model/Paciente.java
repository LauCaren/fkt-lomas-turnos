package ar.edu.uade.recordatorios.model;

public class Paciente {

    private final Long id;
    private final String nombre;
    private final String telefono; // usado para SMS y WhatsApp (formato E.164, ej: +5491122334455)
    private final String email;
    private final CanalNotificacion canalPreferido;

    public Paciente(Long id, String nombre, String telefono, String email, CanalNotificacion canalPreferido) {
        this.id = id;
        this.nombre = nombre;
        this.telefono = telefono;
        this.email = email;
        this.canalPreferido = canalPreferido;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public CanalNotificacion getCanalPreferido() { return canalPreferido; }
}
