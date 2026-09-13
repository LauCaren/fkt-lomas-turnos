-- Foto de perfil para cualquier usuario (paciente, admin, secretaria, médico o contador) —
-- separado de medicos.foto_path/foto_content_type, que es la foto pública de marketing del
-- profesional (equipo.html). Esta es la del perfil personal dentro de la app, privada.
-- avatar_generico guarda la clave de un avatar predefinido (ver AVATARES_VALIDOS en
-- UsuarioFotoService) cuando el usuario elige uno en vez de subir/sacarse una foto real —
-- nullable porque ninguna de las tres cosas es obligatoria: sin elegir nada, se muestra un
-- círculo con las iniciales.
ALTER TABLE usuarios ADD COLUMN foto_path VARCHAR(255);
ALTER TABLE usuarios ADD COLUMN foto_content_type VARCHAR(100);
ALTER TABLE usuarios ADD COLUMN avatar_generico VARCHAR(30);
