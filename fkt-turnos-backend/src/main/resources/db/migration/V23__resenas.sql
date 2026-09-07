CREATE TABLE resenas (
    id BIGSERIAL PRIMARY KEY,
    turno_id BIGINT NOT NULL UNIQUE REFERENCES turnos(id),
    paciente_id BIGINT NOT NULL REFERENCES usuarios(id),
    medico_id BIGINT NOT NULL REFERENCES medicos(id),
    puntaje SMALLINT NOT NULL CHECK (puntaje BETWEEN 1 AND 5),
    comentario VARCHAR(500),
    estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo VARCHAR(300),
    resuelta_por_id BIGINT REFERENCES usuarios(id),
    resuelta_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_resenas_medico ON resenas(medico_id);
CREATE INDEX idx_resenas_estado ON resenas(estado);
