CREATE TABLE registro_auditoria (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT REFERENCES usuarios(id),
    tipo_entidad VARCHAR(50) NOT NULL,
    entidad_id BIGINT NOT NULL,
    operacion VARCHAR(40) NOT NULL,
    snapshot_antes TEXT,
    snapshot_despues TEXT,
    deshecho BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_auditoria_entidad ON registro_auditoria(tipo_entidad, entidad_id);
CREATE INDEX idx_auditoria_created_at ON registro_auditoria(created_at);
