package ar.edu.uade.recordatorios.tools;

import ar.edu.uade.recordatorios.gui.IconoApp;

import java.nio.file.Path;

/**
 * Herramienta de desarrollo, no se usa en tiempo de ejecución de la app.
 * Regenera windows/icon.ico (usado por jpackage al armar el instalador de
 * Windows) a partir del dibujo en IconoApp. Correr con:
 *   mvn -q compile exec:java -Dexec.mainClass=ar.edu.uade.recordatorios.tools.GenerarIcono
 */
public class GenerarIcono {

    public static void main(String[] args) throws Exception {
        Path destino = Path.of("windows", "icon.ico");
        IconoApp.generarIco(destino, 16, 32, 48, 128, 256);
        System.out.println("Ícono generado en " + destino.toAbsolutePath());
    }
}
