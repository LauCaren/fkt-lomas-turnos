-- Profesionales reales listados en equipo.html (FKT Pagina). Sin matrícula ni horarios
-- cargados todavía: eso lo completa Fernando desde el panel de admin.
INSERT INTO medicos (nombre_completo, especialidad_id, activo, duracion_turno_minutos)
SELECT nombre_completo, (SELECT id FROM especialidades WHERE nombre = especialidad), true, 30
FROM (VALUES
    ('Lic. Fernando Carenzio', 'Kinesiología'),
    ('Lic. Diego Palacios', 'Kinesiología'),
    ('Lic. Paola Luna', 'Kinesiología'),
    ('Lic. Agustina Smit', 'Kinesiología'),
    ('Lic. Belén Borgonovo', 'Kinesiología'),
    ('Lic. Florencia Ramírez', 'Kinesiología'),
    ('Lic. Camila Gómez', 'Kinesiología'),
    ('Lic. Fátima Zánchez', 'Kinesiología'),
    ('Dra. Graciela Sanchez', 'Dermopatología')
) AS equipo(nombre_completo, especialidad);
