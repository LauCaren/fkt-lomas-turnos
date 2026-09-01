ALTER TABLE medicos ADD COLUMN usuario_id BIGINT UNIQUE REFERENCES usuarios(id);

ALTER TABLE turnos ADD COLUMN estado_asistencia VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE';

CREATE TABLE evoluciones_clinicas (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES usuarios(id),
    medico_id BIGINT NOT NULL REFERENCES medicos(id),
    turno_id BIGINT NOT NULL REFERENCES turnos(id),
    texto TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_evoluciones_paciente ON evoluciones_clinicas(paciente_id);
CREATE INDEX idx_evoluciones_medico ON evoluciones_clinicas(medico_id);
CREATE UNIQUE INDEX idx_evoluciones_turno ON evoluciones_clinicas(turno_id);
