CREATE TABLE feriados (
    id BIGSERIAL PRIMARY KEY,
    fecha DATE NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL,
    tipo VARCHAR(20) NOT NULL
);

CREATE INDEX idx_feriados_fecha ON feriados (fecha);

-- Calendario oficial de feriados nacionales de Argentina 2026 (Ley 27.399),
-- cruzado contra dos fuentes independientes antes de cargarlo.
INSERT INTO feriados (fecha, nombre, tipo) VALUES
    ('2026-01-01', 'Año Nuevo', 'NACIONAL'),
    ('2026-02-16', 'Carnaval', 'NACIONAL'),
    ('2026-02-17', 'Carnaval', 'NACIONAL'),
    ('2026-03-23', 'Día no laborable con fines turísticos', 'PUENTE'),
    ('2026-03-24', 'Día Nacional de la Memoria por la Verdad y la Justicia', 'NACIONAL'),
    ('2026-04-02', 'Día del Veterano y de los Caídos en la Guerra de Malvinas', 'NACIONAL'),
    ('2026-04-03', 'Viernes Santo', 'NACIONAL'),
    ('2026-05-01', 'Día del Trabajador', 'NACIONAL'),
    ('2026-05-25', 'Día de la Revolución de Mayo', 'NACIONAL'),
    ('2026-06-15', 'Paso a la Inmortalidad del Gral. Martín Miguel de Güemes', 'NACIONAL'),
    ('2026-06-20', 'Paso a la Inmortalidad del Gral. Manuel Belgrano (Día de la Bandera)', 'NACIONAL'),
    ('2026-07-09', 'Día de la Independencia', 'NACIONAL'),
    ('2026-07-10', 'Día no laborable con fines turísticos', 'PUENTE'),
    ('2026-08-17', 'Paso a la Inmortalidad del Gral. José de San Martín', 'NACIONAL'),
    ('2026-10-12', 'Día del Respeto a la Diversidad Cultural', 'NACIONAL'),
    ('2026-11-23', 'Día de la Soberanía Nacional', 'NACIONAL'),
    ('2026-12-07', 'Día no laborable con fines turísticos', 'PUENTE'),
    ('2026-12-08', 'Inmaculada Concepción de María', 'NACIONAL'),
    ('2026-12-25', 'Navidad', 'NACIONAL');
