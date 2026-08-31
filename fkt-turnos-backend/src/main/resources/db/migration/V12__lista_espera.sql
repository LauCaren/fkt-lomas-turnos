CREATE TABLE lista_espera (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    medico_id BIGINT NOT NULL REFERENCES medicos(id) ON DELETE CASCADE,
    fecha DATE NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    notificado_at TIMESTAMPTZ
);

CREATE INDEX idx_lista_espera_medico_fecha_estado ON lista_espera (medico_id, fecha, estado);

-- Evita que el mismo paciente se anote dos veces para el mismo médico/día
-- mientras la anotación siga activa; una vez notificada puede volver a
-- anotarse (por ejemplo si no llegó a tomar el turno liberado).
CREATE UNIQUE INDEX idx_lista_espera_activa_unica ON lista_espera (paciente_id, medico_id, fecha) WHERE estado = 'ACTIVA';
