package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.UsuarioFotoInfoDto;
import com.fktlomas.turnos.exception.ArchivoInvalidoException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
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

    private static final Map<String, String> EXTENSIONES_PERMITIDAS = Map.of("image/jpeg", ".jpg", "image/png", ".png");
    private static final long TAMANO_MAXIMO_BYTES = 5L * 1024 * 1024;

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
        String destino = guardarImagen(foto);
        usuario.setFotoPath(destino);
        usuario.setFotoContentType(foto.getContentType());
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

    private String guardarImagen(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("Adjuntá una imagen");
        }
        String extension = archivo.getContentType() == null ? null : EXTENSIONES_PERMITIDAS.get(archivo.getContentType());
        if (extension == null) {
            throw new ArchivoInvalidoException("La imagen debe ser JPG o PNG");
        }
        if (archivo.getSize() > TAMANO_MAXIMO_BYTES) {
            throw new ArchivoInvalidoException("La imagen no puede superar los 5MB");
        }
        // El Content-Type lo declara el navegador/cliente y se puede falsificar — se verifica acá
        // que el archivo sea realmente una imagen decodificable (mismo chequeo que MedicoService).
        try {
            if (ImageIO.read(archivo.getInputStream()) == null) {
                throw new ArchivoInvalidoException("El archivo no es una imagen válida");
            }
        } catch (IOException e) {
            throw new ArchivoInvalidoException("El archivo no es una imagen válida");
        }
        try {
            return fileStorageService.guardar("usuarios/foto", UUID.randomUUID() + extension, archivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
    }
}
