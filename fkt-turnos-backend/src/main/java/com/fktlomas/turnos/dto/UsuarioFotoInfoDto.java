package com.fktlomas.turnos.dto;

import com.fktlomas.turnos.domain.Usuario;

/** Con qué mostrar el avatar de un usuario: foto real (pedir los bytes a /foto), un avatar
 *  genérico predefinido por clave, o ninguno de los dos (el frontend cae a las iniciales). */
public record UsuarioFotoInfoDto(boolean tieneFoto, String avatarGenerico) {
    public static UsuarioFotoInfoDto de(Usuario u) {
        return new UsuarioFotoInfoDto(u.getFotoPath() != null, u.getAvatarGenerico());
    }
}
