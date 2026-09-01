CREATE TABLE configuracion_caja (
    id SMALLINT PRIMARY KEY,
    email_reporte_mensual VARCHAR(180),
    updated_at TIMESTAMP
);

INSERT INTO configuracion_caja (id, email_reporte_mensual) VALUES (1, NULL);
