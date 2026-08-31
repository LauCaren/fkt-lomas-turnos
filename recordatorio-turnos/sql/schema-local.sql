-- Base propia de la app (H2 embebido). Se crea sola al arrancar, no requiere
-- instalar ni configurar nada. Es un espejo de los pacientes/turnos del
-- sistema real: la sincronización solo agrega y actualiza filas acá, nunca
-- escribe en la base de origen.
--
-- canal_preferido vive ÚNICAMENTE acá: la sincronización lo deja intacto en
-- cada corrida (ver SincronizadorService), así que es lo único que gobierna
-- exclusivamente esta app.
--
-- activo distingue una fila que dejó de aparecer en el sistema de origen
-- (turno cancelado, paciente dado de baja) sin borrar el historial local.

CREATE TABLE IF NOT EXISTS pacientes (
    id BIGINT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    telefono VARCHAR(30),
    email VARCHAR(150),
    canal_preferido ENUM('EMAIL','SMS','WHATSAPP') NOT NULL DEFAULT 'EMAIL',
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS turnos (
    id BIGINT PRIMARY KEY,
    paciente_id BIGINT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL,
    profesional VARCHAR(150),
    especialidad VARCHAR(150),
    consultorio VARCHAR(100),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (paciente_id) REFERENCES pacientes(id)
);

CREATE TABLE IF NOT EXISTS recordatorios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    turno_id BIGINT NOT NULL,
    canal ENUM('EMAIL','SMS','WHATSAPP') NOT NULL,
    estado ENUM('PENDIENTE','ENVIADO','FALLIDO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_envio TIMESTAMP NULL,
    intentos INT NOT NULL DEFAULT 0,
    detalle_error VARCHAR(500),
    FOREIGN KEY (turno_id) REFERENCES turnos(id)
);

-- Una sola fila (id=1): estado de la última sincronización, para mostrar en la GUI.
CREATE TABLE IF NOT EXISTS sincronizacion (
    id INT PRIMARY KEY,
    ultima_ejecucion TIMESTAMP,
    pacientes_sincronizados INT,
    turnos_sincronizados INT,
    detalle_error VARCHAR(500)
);
