package ar.edu.uade.recordatorios.gui;

import javafx.application.Platform;
import javafx.stage.Stage;

import javax.swing.JOptionPane;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

/**
 * Ícono de bandeja del sistema: al cerrar la ventana con la X, la app sigue
 * corriendo en segundo plano (el scheduler sigue mandando recordatorios) y
 * se reabre haciendo click en el ícono. JavaFX todavía no tiene API propia
 * de bandeja, así que se usa java.awt.SystemTray.
 */
public class TrayIconManager {

    private final Stage stage;
    private final Runnable alSalir;
    private TrayIcon trayIcon;

    public TrayIconManager(Stage stage, Runnable alSalir) {
        this.stage = stage;
        this.alSalir = alSalir;
    }

    public void instalar() {
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();

            PopupMenu menu = new PopupMenu();
            MenuItem abrir = new MenuItem("Abrir");
            abrir.addActionListener(e -> Platform.runLater(this::mostrarVentana));
            MenuItem salir = new MenuItem("Salir");
            salir.addActionListener(e -> confirmarSalida());
            menu.add(abrir);
            menu.addSeparator();
            menu.add(salir);

            trayIcon = new TrayIcon(IconoApp.dibujar(16), "Recordatorio de turnos - FKT Lomas", menu);
            trayIcon.setImageAutoSize(true);
            trayIcon.addActionListener(e -> Platform.runLater(this::mostrarVentana));

            tray.add(trayIcon);

            stage.setOnCloseRequest(e -> {
                e.consume();
                stage.hide();
            });
        } catch (AWTException e) {
            throw new RuntimeException("No se pudo instalar el ícono de bandeja del sistema", e);
        }
    }

    /** Cerrar sin querer corta el envío de recordatorios hasta que alguien vuelva a abrir la app — vale la pena confirmar. */
    private void confirmarSalida() {
        int opcion = JOptionPane.showConfirmDialog(null,
            "Si cerrás la app, se van a dejar de mandar recordatorios hasta que la vuelvas a abrir.\n¿Seguro que querés salir?",
            "Recordatorio de turnos - FKT Lomas", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opcion == JOptionPane.YES_OPTION) {
            Platform.runLater(alSalir);
        }
    }

    private void mostrarVentana() {
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    public void remover() {
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
    }

    /** Ej. avisar que hay recordatorios que vienen fallando, sin que nadie tenga que abrir la app para enterarse. */
    public void mostrarNotificacion(String titulo, String mensaje) {
        if (trayIcon != null) {
            trayIcon.displayMessage(titulo, mensaje, TrayIcon.MessageType.WARNING);
        }
    }
}
