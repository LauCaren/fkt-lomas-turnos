package com.fktlomas.turnos.controller;

import com.fktlomas.turnos.dto.UsuarioFotoInfoDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.UsuarioFotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Foto de perfil personal — accesible a cualquier usuario autenticado sobre SU PROPIA cuenta
 * (/me, sin restricción de rol: hasta alguien con el 2FA de staff pendiente de activar tiene
 * que poder elegir su avatar, ver AppUserDetails). Ver "otra" foto (/{id}/foto) es más
 * restrictivo — uno mismo, o cualquiera de los cuatro roles de staff (lo necesitan para ver
 * la ficha de un paciente, o simplemente el nombre+foto de un compañero en otra pantalla).
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioFotoController {

    private final UsuarioFotoService usuarioFotoService;

    @GetMapping("/me/foto-info")
    public UsuarioFotoInfoDto miFotoInfo(@AuthenticationPrincipal AppUserDetails principal) {
        return UsuarioFotoInfoDto.de(principal.getUsuario());
    }

    @PostMapping(value = "/me/foto", consumes = "multipart/form-data")
    public UsuarioFotoInfoDto subirMiFoto(@AuthenticationPrincipal AppUserDetails principal, @RequestParam("foto") MultipartFile foto) {
        return usuarioFotoService.subirFoto(principal.getUsuario(), foto);
    }

    @PostMapping("/me/avatar")
    public UsuarioFotoInfoDto elegirMiAvatar(@AuthenticationPrincipal AppUserDetails principal, @RequestBody Map<String, String> body) {
        return usuarioFotoService.elegirAvatar(principal.getUsuario(), body.get("avatarKey"));
    }

    @DeleteMapping("/me/foto")
    public UsuarioFotoInfoDto quitarMiFoto(@AuthenticationPrincipal AppUserDetails principal) {
        return usuarioFotoService.quitarFoto(principal.getUsuario());
    }

    @GetMapping("/me/foto")
    public ResponseEntity<Resource> miFoto(@AuthenticationPrincipal AppUserDetails principal) {
        return foto(principal.getUsuario().getId());
    }

    @PreAuthorize("#id == authentication.principal.usuario.id or hasAnyRole('ADMIN', 'SECRETARIA', 'MEDICO', 'CONTADOR')")
    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable Long id) {
        Resource recurso = usuarioFotoService.obtenerFoto(id);
        String contentType = usuarioFotoService.obtenerFotoContentType(id);
        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .body(recurso);
    }
}
