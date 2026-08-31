package ar.edu.uade.recordatorios.config;

import java.io.IOException;
import java.util.Locale;

/**
 * Prende/apaga que la app arranque sola al iniciar sesión en Windows, vía
 * la clave de registro HKCU\...\Run (no requiere permisos de administrador).
 *
 * Solo tiene sentido con la app ya instalada (el .exe generado por
 * jpackage): en modo desarrollo ("mvn javafx:run") el proceso en ejecución
 * es java.exe, no el ejecutable final, así que activarlo ahí apuntaría al
 * lugar equivocado — por eso esEjecutableEmpaquetado() lo bloquea.
 */
public class InicioAutomatico {

    private static final String CLAVE_REGISTRO = "RecordatorioFKTLomas";
    private static final String RUTA_REGISTRO = "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";

    private InicioAutomatico() {
    }

    public static boolean esSoportado() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** false si está corriendo desde el jar/dev (java.exe) en vez del instalador. */
    public static boolean esEjecutableEmpaquetado() {
        String comando = rutaEjecutableActual().toLowerCase(Locale.ROOT);
        return !comando.isBlank() && !comando.endsWith("java.exe") && !comando.endsWith("javaw.exe");
    }

    public static boolean estaActivo() {
        try {
            Process proceso = new ProcessBuilder("reg", "query", RUTA_REGISTRO, "/v", CLAVE_REGISTRO)
                .redirectErrorStream(true).start();
            return proceso.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public static void activar() throws IOException, InterruptedException {
        String ejecutable = rutaEjecutableActual();
        ejecutar(new ProcessBuilder("reg", "add", RUTA_REGISTRO,
            "/v", CLAVE_REGISTRO, "/t", "REG_SZ", "/d", ejecutable, "/f"));
    }

    public static void desactivar() throws IOException, InterruptedException {
        ejecutar(new ProcessBuilder("reg", "delete", RUTA_REGISTRO, "/v", CLAVE_REGISTRO, "/f"));
    }

    private static String rutaEjecutableActual() {
        return ProcessHandle.current().info().command().orElse("");
    }

    private static void ejecutar(ProcessBuilder pb) throws IOException, InterruptedException {
        Process proceso = pb.redirectErrorStream(true).start();
        proceso.waitFor();
    }
}
