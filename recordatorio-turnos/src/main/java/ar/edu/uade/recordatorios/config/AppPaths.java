package ar.edu.uade.recordatorios.config;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Carpeta donde esta app guarda SUS propios datos: config/application.properties,
 * la base local H2, los respaldos y el log. No puede resolverse como una ruta
 * relativa al directorio de trabajo del proceso ("./config", etc.) para la GUI
 * empaquetada (jpackage): ni Windows ni macOS garantizan un cwd en particular al
 * abrir la app con doble click (verificado: en Mac, "open" sobre el .app la deja
 * en /tmp), y en Windows el directorio de instalación (ej. "Program Files") ni
 * siquiera es escribible por un usuario sin privilegios de administrador.
 *
 * Por eso, cuando la app corre empaquetada en Windows o Mac, se usa la carpeta
 * estándar de cada sistema operativo para datos de aplicaciones del usuario
 * actual (siempre escribible, siempre la misma sin importar cómo se abrió la
 * app). En los demás casos se mantiene el directorio de trabajo tal cual estaba
 * antes, sin cambios: en desarrollo (IDE, "mvn javafx:run") porque así lo
 * documenta el README, y en la instalación headless de Linux (install.sh) porque
 * el servicio systemd ya fija un WorkingDirectory explícito y escribible.
 */
public final class AppPaths {

    private static final String NOMBRE_APP = "RecordatorioFKTLomas";

    private AppPaths() {
    }

    public static Path directorioBase() {
        String os = System.getProperty("os.name", "").toLowerCase();
        boolean empaquetado = corriendoDesdeJar();

        if (empaquetado && os.contains("win")) {
            String appData = System.getenv("APPDATA");
            Path base = Path.of(appData != null ? appData : System.getProperty("user.home"));
            return base.resolve(NOMBRE_APP);
        }
        if (empaquetado && os.contains("mac")) {
            return Path.of(System.getProperty("user.home"), "Library", "Application Support", NOMBRE_APP);
        }
        return Path.of("").toAbsolutePath();
    }

    /** true si esta clase se está ejecutando desde un .jar (empaquetada), no desde .class sueltos (dev/IDE). */
    private static boolean corriendoDesdeJar() {
        try {
            Path origen = Path.of(AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return Files.isRegularFile(origen);
        } catch (Exception e) {
            return false;
        }
    }
}
