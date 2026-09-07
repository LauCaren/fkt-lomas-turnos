-- V23 creó puntaje como SMALLINT pero la entidad Resena.java lo mapea como int (INTEGER
-- por default de Hibernate), lo que rompe la validación de esquema al arrancar.
ALTER TABLE resenas ALTER COLUMN puntaje TYPE integer;
