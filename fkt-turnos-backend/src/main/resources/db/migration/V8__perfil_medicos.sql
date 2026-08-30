-- Campos opcionales para la ficha pública de cada profesional (equipo.html):
-- se van completando de a poco desde el panel de admin, por eso todo nullable
-- y sin default. Un médico sin estos datos simplemente no muestra esa parte
-- de la ficha (ver MedicoController/MedicoDto).
ALTER TABLE medicos
    ADD COLUMN anos_experiencia INT,
    ADD COLUMN foto_path VARCHAR(255),
    ADD COLUMN foto_content_type VARCHAR(100),
    ADD COLUMN titulo_imagen_path VARCHAR(255),
    ADD COLUMN titulo_imagen_content_type VARCHAR(100);
