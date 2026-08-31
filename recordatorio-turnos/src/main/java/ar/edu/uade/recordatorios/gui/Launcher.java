package ar.edu.uade.recordatorios.gui;

/**
 * Punto de entrada real del ejecutable empaquetado (jpackage). No puede ser
 * RecordatorioApp directamente: cuando la clase main() extiende
 * javafx.application.Application y se lanza por classpath (no por módulos,
 * que es como la empaqueta jpackage acá), el propio launcher de la JVM
 * rechaza el arranque con "Error: faltan los componentes de JavaFX runtime y
 * son necesarios para ejecutar esta aplicación" aunque JavaFX esté
 * perfectamente presente — es una limitación conocida de JavaFX en modo
 * classpath. La solución es indireccionar el main() a través de una clase
 * que NO extienda Application.
 */
public class Launcher {

    public static void main(String[] args) {
        RecordatorioApp.main(args);
    }
}
