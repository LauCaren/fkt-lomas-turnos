package ar.edu.uade.recordatorios.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Lee y escribe config/application.properties para la pantalla de
 * Configuración de la GUI. A diferencia de AppConfig (que exige que el
 * archivo exista y esté completo para arrancar el servicio headless), acá
 * un archivo ausente o incompleto es una situación normal: significa que
 * todavía no se configuró la app desde la interfaz.
 */
public class ConfigStore {

    public static final Path RUTA = AppPaths.directorioBase().resolve("config").resolve("application.properties");

    public static Properties cargar() {
        Properties props = new Properties();
        if (Files.isReadable(RUTA)) {
            try (InputStream in = Files.newInputStream(RUTA)) {
                props.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Error leyendo " + RUTA, e);
            }
        }
        return props;
    }

    public static void guardar(Properties props) {
        try {
            Path carpeta = RUTA.getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            try (OutputStream out = Files.newOutputStream(RUTA)) {
                props.store(out, "Configuración de Recordatorio de turnos - FKT Lomas");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error guardando " + RUTA, e);
        }
    }
}
