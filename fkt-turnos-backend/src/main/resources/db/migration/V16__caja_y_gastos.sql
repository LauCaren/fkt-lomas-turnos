ALTER TABLE medicos ADD COLUMN tarifa_consulta NUMERIC(12,2);

CREATE TABLE categorias_gasto (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO categorias_gasto (nombre) VALUES
    ('Luz'), ('Gas'), ('Agua'), ('Alquiler'), ('Insumos'), ('Limpieza'), ('Sueldos'), ('Otros');

CREATE TABLE ingresos (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL REFERENCES medicos(id),
    turno_id BIGINT REFERENCES turnos(id),
    monto NUMERIC(12,2) NOT NULL,
    metodo_pago VARCHAR(20) NOT NULL,
    fecha DATE NOT NULL,
    registrado_por_id BIGINT NOT NULL REFERENCES usuarios(id),
    anulado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(300),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE retiros (
    id BIGSERIAL PRIMARY KEY,
    medico_id BIGINT NOT NULL REFERENCES medicos(id),
    monto NUMERIC(12,2) NOT NULL,
    entregado_por_id BIGINT NOT NULL REFERENCES usuarios(id),
    fecha_hora TIMESTAMP NOT NULL,
    anulado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(300)
);

CREATE TABLE gastos (
    id BIGSERIAL PRIMARY KEY,
    categoria_id BIGINT NOT NULL REFERENCES categorias_gasto(id),
    monto NUMERIC(12,2) NOT NULL,
    fecha DATE NOT NULL,
    descripcion VARCHAR(500),
    comprobante_path VARCHAR(500),
    comprobante_content_type VARCHAR(100),
    registrado_por_id BIGINT NOT NULL REFERENCES usuarios(id),
    anulado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_anulacion VARCHAR(300),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_ingresos_medico ON ingresos(medico_id);
CREATE INDEX idx_ingresos_fecha ON ingresos(fecha);
CREATE INDEX idx_retiros_medico ON retiros(medico_id);
CREATE INDEX idx_gastos_fecha ON gastos(fecha);
