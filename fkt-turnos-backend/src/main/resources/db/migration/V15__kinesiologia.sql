ALTER TABLE especialidades ADD COLUMN es_kinesiologia BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE especialidades SET es_kinesiologia = TRUE WHERE nombre = 'Kinesiología';

CREATE TABLE planes_kinesiologia (
    id BIGSERIAL PRIMARY KEY,
    paciente_id BIGINT NOT NULL REFERENCES usuarios(id),
    medico_id BIGINT NOT NULL REFERENCES medicos(id),
    cantidad_sesiones INT NOT NULL,
    sesiones_generadas INT NOT NULL DEFAULT 0,
    fecha_inicio DATE NOT NULL,
    estado VARCHAR(25) NOT NULL DEFAULT 'PENDIENTE_VALIDACION',
    orden_medica_path VARCHAR(500) NOT NULL,
    orden_medica_content_type VARCHAR(100),
    motivo_rechazo VARCHAR(500),
    validado_por_id BIGINT REFERENCES usuarios(id),
    validado_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE planes_kinesiologia_horarios (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES planes_kinesiologia(id) ON DELETE CASCADE,
    dia_semana VARCHAR(15) NOT NULL,
    hora_inicio TIME NOT NULL
);

CREATE INDEX idx_planes_kinesiologia_paciente ON planes_kinesiologia(paciente_id);
CREATE INDEX idx_planes_kinesiologia_estado ON planes_kinesiologia(estado);

ALTER TABLE turnos ADD COLUMN plan_kinesiologia_id BIGINT REFERENCES planes_kinesiologia(id);
CREATE INDEX idx_turnos_plan_kinesiologia ON turnos(plan_kinesiologia_id);
