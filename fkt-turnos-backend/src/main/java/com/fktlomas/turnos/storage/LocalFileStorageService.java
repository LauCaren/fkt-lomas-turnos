package com.fktlomas.turnos.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Storage en disco local — el comportamiento de siempre, usado en dev y salvo que se
 * configure explícitamente {@code app.storage.type=s3}.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @Override
    public String guardar(String carpeta, String nombreArchivo, MultipartFile archivo) throws IOException {
        Path directorio = Path.of(uploadsDir, carpeta);
        Files.createDirectories(directorio);
        Path destino = directorio.resolve(nombreArchivo);
        archivo.transferTo(destino);
        return destino.toString();
    }

    @Override
    public String guardar(String carpeta, String nombreArchivo, byte[] contenido, String contentType) throws IOException {
        Path directorio = Path.of(uploadsDir, carpeta);
        Files.createDirectories(directorio);
        Path destino = directorio.resolve(nombreArchivo);
        Files.write(destino, contenido);
        return destino.toString();
    }

    @Override
    public Optional<Resource> cargar(String clave) {
        if (clave == null) {
            return Optional.empty();
        }
        Path path = Path.of(clave);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(path));
    }

    @Override
    public void borrar(String clave) {
        if (clave == null) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(clave));
        } catch (IOException ignored) {
            // No es crítico: queda un archivo huérfano en disco, no afecta el funcionamiento.
        }
    }
}
