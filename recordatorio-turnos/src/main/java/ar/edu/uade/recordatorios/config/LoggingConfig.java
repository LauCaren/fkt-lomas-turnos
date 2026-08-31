package ar.edu.uade.recordatorios.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Además de la consola, escribe los logs a ./logs/app.log (con rotación).
 * Necesario sobre todo para la GUI de Windows: cuando se abre con doble
 * click no hay consola visible, así que sin esto un error de madrugada
 * sería invisible para cualquiera que quiera diagnosticarlo después.
 */
public class LoggingConfig {

    private static final int TAMANIO_MAXIMO_BYTES = 2_000_000;
    private static final int ARCHIVOS_A_ROTAR = 5;

    private LoggingConfig() {
    }

    public static void inicializar() {
        try {
            Path carpetaLogs = AppPaths.directorioBase().resolve("logs");
            Files.createDirectories(carpetaLogs);
            FileHandler fileHandler = new FileHandler(
                carpetaLogs.resolve("app.log").toString(), TAMANIO_MAXIMO_BYTES, ARCHIVOS_A_ROTAR, true);
            fileHandler.setFormatter(new SimpleFormatter());
            fileHandler.setLevel(Level.ALL);

            Logger root = Logger.getLogger("");
            root.addHandler(fileHandler);
        } catch (IOException e) {
            System.err.println("No se pudo inicializar el log a archivo: " + e.getMessage());
        }
    }
}
