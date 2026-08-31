package com.fktlomas.turnos.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Abstrae dónde viven los archivos subidos (fotos de médicos, títulos, CVs).
 * En dev es disco local; en producción, un bucket S3-compatible (ver {@link S3FileStorageService})
 * porque el host de despliegue no garantiza disco persistente.
 */
public interface FileStorageService {

    /**
     * Guarda el archivo bajo la carpeta lógica indicada y devuelve la clave con la que
     * hay que pedirlo después (se persiste tal cual en la base — nunca es una URL pública).
     */
    String guardar(String carpeta, String nombreArchivo, MultipartFile archivo) throws IOException;

    Optional<Resource> cargar(String clave);

    /** Best-effort: si falla o el archivo no existe, no rompe el flujo que la llama. */
    void borrar(String clave);
}
