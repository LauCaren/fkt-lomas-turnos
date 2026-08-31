package ar.edu.uade.recordatorios.config;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Evita que se abran dos instancias de la app al mismo tiempo (ej. alguien
 * hace doble click de más en el ícono): sin esto, cada instancia correría
 * su propio scheduler y podría duplicar envíos. El lock se libera solo al
 * cerrar el proceso, incluso si se cierra de mala manera.
 */
public class InstanceLock {

    private static FileChannel canal;
    private static FileLock lock;

    private InstanceLock() {
    }

    /** true si se pudo tomar el lock (esta es la única instancia corriendo). */
    public static boolean adquirir() {
        try {
            Path carpetaDatos = AppPaths.directorioBase().resolve("data");
            Files.createDirectories(carpetaDatos);
            canal = FileChannel.open(carpetaDatos.resolve(".lock"),
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = canal.tryLock();
            return lock != null;
        } catch (IOException | OverlappingFileLockException e) {
            return false;
        }
    }
}
