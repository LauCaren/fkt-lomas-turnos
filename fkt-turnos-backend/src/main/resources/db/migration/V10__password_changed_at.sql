-- Para poder invalidar tokens JWT emitidos antes de un cambio de contraseña
-- (ver JwtAuthFilter): si alguien cambia su contraseña, cualquier token viejo
-- que ande dando vueltas deja de servir en el acto, en vez de seguir válido
-- hasta que expire solo (hasta 7 días después).
ALTER TABLE usuarios ADD COLUMN password_changed_at TIMESTAMP NOT NULL DEFAULT now();
