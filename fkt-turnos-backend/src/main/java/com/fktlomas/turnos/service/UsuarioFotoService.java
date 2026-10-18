package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.UsuarioFotoInfoDto;
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import com.fktlomas.turnos.storage.ImagenSegura;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Set;
import java.util.UUID;

/**
 * Foto de perfil de CUALQUIER usuario (paciente, admin, secretaria, médico o contador) —
 * cada uno la administra sobre su propia cuenta desde /api/usuarios/me/**. Distinto de
 * MedicoService.subirFoto/subirTitulo, que es la foto pública de marketing del profesional
 * (equipo.html), no la del perfil personal dentro de la app.
 */
@Service
@RequiredArgsConstructor
public class UsuarioFotoService {

    /** Ocho opciones fijas — el frontend las dibuja localmente (constants/avataresGenericos.js),
     *  acá solo se valida que la clave elegida sea una de ellas antes de guardarla. */
    private static final Set<String> AVATARES_VALIDOS = Set.of(
            "avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5", "avatar_6", "avatar_7", "avatar_8"
    );

    private final UsuarioRepository usuarioRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public UsuarioFotoInfoDto info(Long id) {
        return UsuarioFotoInfoDto.de(obtenerUsuario(id));
    }

    @Transactional
    public UsuarioFotoInfoDto subirFoto(Usuario usuario, MultipartFile foto) {
        String rutaAnterior = usuario.getFotoPath();
        ImagenGuardada guardada = guardarImagen(foto);
        usuario.setFotoPath(guardada.ruta());
        usuario.setFotoContentType(guardada.contentType());
        usuario.setAvatarGenerico(null);
        usuarioRepository.save(usuario);
        fileStorageService.borrar(rutaAnterior);
        return UsuarioFotoInfoDto.de(usuario);
    }

    @Transactional
    public UsuarioFotoInfoDto elegirAvatar(Usuario usuario, String avatarKey) {
        if (avatarKey == null || !AVATARES_VALIDOS.contains(avatarKey)) {
            throw new ArchivoInvalidoException("Elegí uno de los avatares disponibles");
        }
        String rutaAnterior = usuario.getFotoPath();
        usuario.setFotoPath(null);
        usuario.setFotoContentType(null);
        usuario.setAvatarGenerico(avatarKey);
        usuarioRepository.save(usuario);
        fileStorageService.borrar(rutaAnterior);
        return UsuarioFotoInfoDto.de(usuario);
    }

    @Transactional
    public UsuarioFotoInfoDto quitarFoto(Usuario usuario) {
        String rutaAnterior = usuario.getFotoPath();
        usuario.setFotoPath(null);
        usuario.setFotoContentType(null);
        usuario.setAvatarGenerico(null);
        usuarioRepository.save(usuario);
        fileStorageService.borrar(rutaAnterior);
        return UsuarioFotoInfoDto.de(usuario);
    }

    public Resource obtenerFoto(Long id) {
        Usuario usuario = obtenerUsuario(id);
        if (usuario.getFotoPath() == null) {
            throw new RecursoNoEncontradoException("Este usuario todavía no tiene una foto cargada");
        }
        return fileStorageService.cargar(usuario.getFotoPath())
                .orElseThrow(() -> new RecursoNoEncontradoException("Este usuario todavía no tiene una foto cargada"));
    }

    public String obtenerFotoContentType(Long id) {
        return obtenerUsuario(id).getFotoContentType();
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private ImagenGuardada guardarImagen(MultipartFile archivo) {
        // Validación + re-codificación (sin EXIF/GPS, con tope de dimensiones): ver ImagenSegura.
        ImagenSegura.Imagen imagen = ImagenSegura.procesar(archivo);
        try {
            String ruta = fileStorageService.guardar("usuarios/foto", UUID.randomUUID() + imagen.extension(),
                    imagen.contenido(), imagen.contentType());
            return new ImagenGuardada(ruta, imagen.contentType());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }

    /** Dónde quedó la imagen y con qué Content-Type se guardó (el que se sirve después). */
    private record ImagenGuardada(String ruta, String contentType) {
    }
}
