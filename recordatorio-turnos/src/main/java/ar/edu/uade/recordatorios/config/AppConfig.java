package ar.edu.uade.recordatorios.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Carga la configuración desde un archivo externo (para poder instalar el
 * programa y ajustarlo sin recompilar), y si no lo encuentra cae al archivo
 * empaquetado en el jar, útil para correrlo desde el IDE en desarrollo.
 *
 * Ubicación del archivo externo, en orden de prioridad:
 *   1. Propiedad de sistema -Drecordatorio.config=/ruta/al/archivo
 *   2. ./config/application.properties (relativo al directorio de trabajo)
 */
public class AppConfig {

    private final Properties props = new Properties();

    public AppConfig(String archivo) {
        Path externo = resolverArchivoExterno(archivo);

        if (externo != null) {
            try (InputStream in = Files.newInputStream(externo)) {
                props.load(in);
                return;
            } catch (IOException e) {
                throw new RuntimeException("Error cargando configuración desde " + externo, e);
            }
        }

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(archivo)) {
            if (in == null) {
                throw new RuntimeException("No se encontró el archivo de configuración: " + archivo
                    + " (ni en config/application.properties ni en el jar)");
            }
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Error cargando configuración", e);
        }
    }

    private Path resolverArchivoExterno(String archivo) {
        String rutaExplicita = System.getProperty("recordatorio.config");
        if (rutaExplicita != null) {
            Path path = Path.of(rutaExplicita);
            if (Files.isReadable(path)) {
                return path;
            }
            throw new RuntimeException("Se indicó -Drecordatorio.config=" + rutaExplicita
                + " pero el archivo no existe o no se puede leer");
        }

        Path porDefecto = AppPaths.directorioBase().resolve("config").resolve(archivo);
        return Files.isReadable(porDefecto) ? porDefecto : null;
    }

    public String get(String clave) {
        String valor = props.getProperty(clave);
        if (valor == null) {
            throw new RuntimeException("Falta la propiedad de configuración: " + clave);
        }
        return valor;
    }

    public int getInt(String clave) {
        return Integer.parseInt(get(clave));
    }

    public String getOrDefault(String clave, String porDefecto) {
        return props.getProperty(clave, porDefecto);
    }
}
