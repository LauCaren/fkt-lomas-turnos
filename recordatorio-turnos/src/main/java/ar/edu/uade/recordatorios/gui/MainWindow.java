package ar.edu.uade.recordatorios.gui;

import ar.edu.uade.recordatorios.config.InicioAutomatico;
import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.EstadoRecordatorio;
import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Recordatorio;
import ar.edu.uade.recordatorios.notification.PlantillaMensaje;
import ar.edu.uade.recordatorios.service.RecordatorioService;
import ar.edu.uade.recordatorios.sync.EstadoSincronizacion;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class MainWindow {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final String VERSION = "1.0.0"; // mantener en sync con la <version> del pom.xml

    private final RecordatorioApp app;
    private final ObservableList<Recordatorio> datos = FXCollections.observableArrayList();
    private final ObservableList<Paciente> datosPacientes = FXCollections.observableArrayList();
    private FilteredList<Recordatorio> datosFiltrados;
    private FilteredList<Paciente> datosPacientesFiltrados;
    private RecordatorioService servicio;

    private TableView<Recordatorio> tabla;
    private TableView<Paciente> tablaPacientes;
    private Label estadoServicioLabel;
    private Button chequearAhoraBtn;
    private Button reintentarBtn;
    private Label configEstadoLabel;
    private Label valorEnviados, valorPendientes, valorFallidos;
    private Label ultimaSincronizacionLabel;

    private TextField dbUrl, dbUsuario, smtpHost, smtpUsuario, twilioSid, numeroSms, numeroWhatsApp,
        horasAnticipacion, intervaloMinutos;
    private PasswordField dbPassword, smtpPassword, twilioToken;
    private TextArea plantillaMensajeArea;
    private Label vistaPreviaLabel;
    private CheckBox inicioAutomaticoCheck;

    public MainWindow(RecordatorioApp app) {
        this.app = app;
    }

    public void mostrar(Stage stage) {
        estadoServicioLabel = new Label();
        estadoServicioLabel.getStyleClass().add("estado-header");
        Node encabezado = crearEncabezado();

        Node paneRecordatorios = crearPestañaRecordatorios();
        Node panePacientes = crearPestañaPacientes();
        Node paneMensaje = crearPestañaMensaje();
        Node paneConfiguracion = crearPestañaConfiguracion();
        mostrarServicioActivo(false, "Completá la configuración para iniciar el servicio.");

        StackPane contenido = new StackPane(paneRecordatorios, panePacientes, paneMensaje, paneConfiguracion);

        Button navRecordatorios = crearBotonNav("🔔   Recordatorios");
        Button navPacientes = crearBotonNav("🧑   Pacientes");
        Button navMensaje = crearBotonNav("✉️   Mensaje");
        Button navConfig = crearBotonNav("⚙   Configuración");
        List<Button> botonesNav = List.of(navRecordatorios, navPacientes, navMensaje, navConfig);
        Map<Button, Node> paneles = Map.of(
            navRecordatorios, paneRecordatorios,
            navPacientes, panePacientes,
            navMensaje, paneMensaje,
            navConfig, paneConfiguracion);

        for (Button boton : botonesNav) {
            boton.setOnAction(e -> seleccionarSeccion(boton, botonesNav, paneles));
        }
        seleccionarSeccion(navRecordatorios, botonesNav, paneles);

        Region espacioSidebar = new Region();
        VBox.setVgrow(espacioSidebar, Priority.ALWAYS);

        Hyperlink acercaDeLink = new Hyperlink("Acerca de");
        acercaDeLink.setOnAction(e -> mostrarAcercaDe());

        VBox sidebar = new VBox(4, navRecordatorios, navPacientes, navMensaje, navConfig, espacioSidebar, acercaDeLink);
        sidebar.getStyleClass().add("sidebar");

        BorderPane raiz = new BorderPane();
        raiz.setTop(encabezado);
        raiz.setLeft(sidebar);
        raiz.setCenter(contenido);

        Scene escena = new Scene(raiz, 980, 660);
        escena.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());

        stage.setTitle("Recordatorio de turnos - FKT Lomas");
        stage.setScene(escena);
        stage.setMinWidth(860);
        stage.setMinHeight(560);
        stage.show();

        FadeTransition fade = new FadeTransition(Duration.millis(280), raiz);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();

        Timeline autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> {
            refrescarTabla();
            refrescarPacientes();
        }));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }

    private Node crearEncabezado() {
        HBox marca = new HBox();
        marca.getStyleClass().add("marca-circulo");
        marca.setAlignment(Pos.CENTER);
        marca.getChildren().add(new Label("FKT"));

        VBox textos = new VBox(1,
            etiqueta("Recordatorio de turnos", null),
            etiqueta("FKT Lomas · Kinesiología y Especialidades Médicas", "subtitulo"));

        HBox izquierda = new HBox(14, marca, textos);
        izquierda.setAlignment(Pos.CENTER_LEFT);

        HBox encabezado = new HBox(izquierda, espaciador(), estadoServicioLabel);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        encabezado.getStyleClass().add("encabezado");
        return encabezado;
    }

    private Button crearBotonNav(String texto) {
        Button boton = new Button(texto);
        boton.getStyleClass().add("nav-item");
        boton.setMaxWidth(Double.MAX_VALUE);
        return boton;
    }

    private void seleccionarSeccion(Button activo, List<Button> botones, Map<Button, Node> paneles) {
        for (Button boton : botones) {
            boton.getStyleClass().remove("nav-activo");
        }
        activo.getStyleClass().add("nav-activo");
        Node panelActivo = paneles.get(activo);
        for (Node panel : paneles.values()) {
            panel.setVisible(panel == panelActivo);
            panel.setManaged(panel == panelActivo);
        }
    }

    // ---------- Pestaña Recordatorios ----------

    private Node crearPestañaRecordatorios() {
        tabla = crearTabla();
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Button refrescarBtn = new Button("Actualizar");
        refrescarBtn.getStyleClass().add("boton-secundario");
        refrescarBtn.setOnAction(e -> refrescarTabla());

        reintentarBtn = new Button("Reintentar seleccionados");
        reintentarBtn.getStyleClass().add("boton-secundario");
        reintentarBtn.setDisable(true);
        reintentarBtn.setOnAction(e -> reintentarSeleccionado());

        chequearAhoraBtn = new Button("Sincronizar y chequear ahora");
        chequearAhoraBtn.getStyleClass().add("boton-primario");
        chequearAhoraBtn.setDisable(true);
        chequearAhoraBtn.setOnAction(e -> chequearAhora());

        TextField buscarField = new TextField();
        buscarField.setPromptText("Buscar paciente...");
        buscarField.setPrefWidth(200);
        buscarField.textProperty().addListener((obs, viejo, nuevo) -> {
            String filtro = nuevo == null ? "" : nuevo.trim().toLowerCase();
            datosFiltrados.setPredicate(r -> filtro.isEmpty()
                || r.getTurno().getPaciente().getNombre().toLowerCase().contains(filtro));
        });

        ultimaSincronizacionLabel = new Label("Sin sincronizar todavía");
        ultimaSincronizacionLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11.5px;");
        ultimaSincronizacionLabel.setPadding(new Insets(0, 24, 0, 24));

        HBox barraAcciones = new HBox(10, buscarField, espaciador(), refrescarBtn, reintentarBtn, chequearAhoraBtn);
        barraAcciones.setAlignment(Pos.CENTER_LEFT);
        barraAcciones.setPadding(new Insets(10, 24, 4, 24));

        VBox contenedor = new VBox(crearTarjetas(), ultimaSincronizacionLabel, barraAcciones, tabla);
        return contenedor;
    }

    private Node crearTarjetas() {
        valorEnviados = new Label("0");
        valorPendientes = new Label("0");
        valorFallidos = new Label("0");

        HBox fila = new HBox(14,
            tarjeta("Enviados", valorEnviados, "tarjeta-enviados"),
            tarjeta("Pendientes", valorPendientes, "tarjeta-pendientes"),
            tarjeta("Fallidos", valorFallidos, "tarjeta-fallidos"));
        fila.getStyleClass().add("tarjetas");

        datos.addListener((javafx.collections.ListChangeListener<Recordatorio>) c -> actualizarTarjetas());
        return fila;
    }

    private VBox tarjeta(String etiqueta, Label valor, String estilo) {
        valor.getStyleClass().add("tarjeta-valor");
        Label label = new Label(etiqueta);
        label.getStyleClass().add("tarjeta-etiqueta");
        VBox caja = new VBox(4, valor, label);
        caja.getStyleClass().addAll("tarjeta", estilo);
        HBox.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }

    private void actualizarTarjetas() {
        long enviados = datos.stream().filter(r -> r.getEstado() == EstadoRecordatorio.ENVIADO).count();
        long pendientes = datos.stream().filter(r -> r.getEstado() == EstadoRecordatorio.PENDIENTE).count();
        long fallidos = datos.stream().filter(r -> r.getEstado() == EstadoRecordatorio.FALLIDO).count();
        valorEnviados.setText(String.valueOf(enviados));
        valorPendientes.setText(String.valueOf(pendientes));
        valorFallidos.setText(String.valueOf(fallidos));
    }

    private TableView<Recordatorio> crearTabla() {
        datosFiltrados = new FilteredList<>(datos, r -> true);
        SortedList<Recordatorio> ordenados = new SortedList<>(datosFiltrados);

        TableView<Recordatorio> t = new TableView<>(ordenados);
        ordenados.comparatorProperty().bind(t.comparatorProperty());
        t.setPlaceholder(new Label("Todavía no hay recordatorios registrados"));
        t.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<Recordatorio, String> colPaciente = new TableColumn<>("Paciente");
        colPaciente.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTurno().getPaciente().getNombre()));

        TableColumn<Recordatorio, String> colTurno = new TableColumn<>("Turno");
        colTurno.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getTurno().getFechaHora().format(FORMATO_FECHA)
                + " · " + valorSeguro(cd.getValue().getTurno().getProfesional())));

        TableColumn<Recordatorio, String> colCanal = new TableColumn<>("Canal");
        colCanal.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCanal().toString()));

        TableColumn<Recordatorio, EstadoRecordatorio> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getEstado()));
        colEstado.setCellFactory(col -> celdaPillEstado());

        TableColumn<Recordatorio, Number> colIntentos = new TableColumn<>("Intentos");
        colIntentos.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getIntentos()));
        colIntentos.setPrefWidth(70);

        TableColumn<Recordatorio, String> colEnviado = new TableColumn<>("Enviado");
        colEnviado.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getFechaEnvio() != null ? cd.getValue().getFechaEnvio().format(FORMATO_FECHA) : "-"));

        TableColumn<Recordatorio, String> colError = new TableColumn<>("Detalle");
        colError.setCellValueFactory(cd -> new SimpleStringProperty(valorSeguro(cd.getValue().getDetalleError())));
        colError.setPrefWidth(240);

        t.getColumns().addAll(colPaciente, colTurno, colCanal, colEstado, colIntentos, colEnviado, colError);
        t.getSelectionModel().getSelectedItems().addListener((javafx.collections.ListChangeListener<Recordatorio>) c ->
            reintentarBtn.setDisable(t.getSelectionModel().getSelectedItems().isEmpty() || servicio == null));
        return t;
    }

    private TableCell<Recordatorio, EstadoRecordatorio> celdaPillEstado() {
        return new TableCell<>() {
            @Override
            protected void updateItem(EstadoRecordatorio estado, boolean vacio) {
                super.updateItem(estado, vacio);
                if (vacio || estado == null) {
                    setGraphic(null);
                    return;
                }
                Label pill = new Label(estado.toString());
                pill.getStyleClass().add("pill");
                pill.getStyleClass().add(switch (estado) {
                    case ENVIADO -> "pill-enviado";
                    case PENDIENTE -> "pill-pendiente";
                    case FALLIDO -> "pill-fallido";
                });
                setGraphic(pill);
            }
        };
    }

    private void refrescarTabla() {
        if (servicio == null) {
            return;
        }
        ejecutarEnFondo(
            () -> servicio.listarUltimos(200),
            datos::setAll,
            error -> mostrarError("No se pudo actualizar la lista de recordatorios", error)
        );
        ejecutarEnFondo(
            () -> servicio.obtenerEstadoSincronizacion(),
            this::mostrarEstadoSincronizacion,
            error -> { /* si falla la lectura del estado, se deja el texto anterior */ }
        );
    }

    private void mostrarEstadoSincronizacion(EstadoSincronizacion estado) {
        if (estado.ultimaEjecucion() == null) {
            ultimaSincronizacionLabel.setText("Todavía no sincronizó con el sistema de turnos.");
            return;
        }
        String cuando = estado.ultimaEjecucion().format(FORMATO_FECHA);
        if (estado.fueExitosa()) {
            ultimaSincronizacionLabel.setText(String.format(
                "Última sincronización: %s — %d pacientes, %d turnos",
                cuando, estado.pacientesSincronizados(), estado.turnosSincronizados()));
        } else {
            ultimaSincronizacionLabel.setText(
                "Falló la última sincronización (" + cuando + "): " + estado.detalleError());
        }
    }

    private void reintentarSeleccionado() {
        List<Recordatorio> seleccionados = tabla.getSelectionModel().getSelectedItems().stream()
            .filter(r -> r.getEstado() != EstadoRecordatorio.ENVIADO)
            .toList();
        if (seleccionados.isEmpty() || servicio == null) {
            return;
        }
        reintentarBtn.setDisable(true);
        ejecutarEnFondo(
            () -> {
                int errores = 0;
                for (Recordatorio r : seleccionados) {
                    try {
                        servicio.reintentarRecordatorio(r.getId());
                    } catch (Exception e) {
                        errores++;
                    }
                }
                return errores;
            },
            errores -> {
                reintentarBtn.setDisable(false);
                refrescarTabla();
                if (errores > 0) {
                    mostrarInfo("Reintento", errores + " de " + seleccionados.size()
                        + " recordatorio(s) no se pudieron reenviar. Revisá el detalle en la tabla.");
                }
            },
            error -> { reintentarBtn.setDisable(false); mostrarError("No se pudo reintentar el envío", error); }
        );
    }

    private void chequearAhora() {
        if (servicio == null) {
            return;
        }
        int horas = parseIntSeguro(horasAnticipacion.getText(), 24);
        chequearAhoraBtn.setDisable(true);
        ejecutarEnFondo(
            () -> { servicio.procesarRecordatoriosPendientes(horas); return null; },
            v -> { chequearAhoraBtn.setDisable(false); refrescarTabla(); },
            error -> { chequearAhoraBtn.setDisable(false); mostrarError("Falló el chequeo manual", error); }
        );
    }

    public void setServicio(RecordatorioService servicio) {
        this.servicio = servicio;
        if (servicio != null) {
            refrescarTabla();
            refrescarPacientes();
        } else {
            datos.clear();
            datosPacientes.clear();
        }
        if (reintentarBtn != null) {
            reintentarBtn.setDisable(true);
        }
    }

    public void mostrarServicioActivo(boolean activo, String mensaje) {
        estadoServicioLabel.getStyleClass().removeAll("estado-ok", "estado-error");
        if (activo) {
            estadoServicioLabel.setText("● Servicio activo");
            estadoServicioLabel.getStyleClass().add("estado-ok");
        } else {
            estadoServicioLabel.setText("● Servicio detenido" + (mensaje != null ? " — " + mensaje : ""));
            estadoServicioLabel.getStyleClass().add("estado-error");
        }
        if (chequearAhoraBtn != null) {
            chequearAhoraBtn.setDisable(!activo);
        }
    }

    // ---------- Pestaña Pacientes ----------

    private Node crearPestañaPacientes() {
        datosPacientesFiltrados = new FilteredList<>(datosPacientes, p -> true);
        SortedList<Paciente> pacientesOrdenados = new SortedList<>(datosPacientesFiltrados);

        tablaPacientes = new TableView<>(pacientesOrdenados);
        pacientesOrdenados.comparatorProperty().bind(tablaPacientes.comparatorProperty());
        tablaPacientes.setEditable(true);
        tablaPacientes.setPlaceholder(new Label("Todavía no hay pacientes cargados"));
        VBox.setVgrow(tablaPacientes, Priority.ALWAYS);

        TableColumn<Paciente, String> colNombre = new TableColumn<>("Paciente");
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getNombre()));
        colNombre.setPrefWidth(220);

        TableColumn<Paciente, String> colTelefono = new TableColumn<>("Teléfono");
        colTelefono.setCellValueFactory(cd -> new SimpleStringProperty(valorSeguro(cd.getValue().getTelefono())));

        TableColumn<Paciente, String> colEmail = new TableColumn<>("Email");
        colEmail.setCellValueFactory(cd -> new SimpleStringProperty(valorSeguro(cd.getValue().getEmail())));
        colEmail.setPrefWidth(220);

        TableColumn<Paciente, CanalNotificacion> colCanal = new TableColumn<>("Vía del recordatorio");
        colCanal.setCellValueFactory(cd -> new SimpleObjectProperty<>(cd.getValue().getCanalPreferido()));
        colCanal.setCellFactory(ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(CanalNotificacion.values())));
        colCanal.setEditable(true);
        colCanal.setPrefWidth(180);
        colCanal.setOnEditCommit(event ->
            actualizarCanalPaciente(event.getRowValue().getId(), event.getNewValue()));

        tablaPacientes.getColumns().addAll(colNombre, colTelefono, colEmail, colCanal);

        Label ayuda = new Label("Doble click en \"Vía del recordatorio\" para cambiar el canal de un paciente.");
        ayuda.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11.5px;");

        Button mensajeBtn = new Button("Enviar mensaje manual");
        mensajeBtn.getStyleClass().add("boton-primario");
        mensajeBtn.setDisable(true);
        mensajeBtn.setOnAction(e -> {
            Paciente seleccionado = tablaPacientes.getSelectionModel().getSelectedItem();
            if (seleccionado != null) {
                mostrarDialogoMensaje(seleccionado);
            }
        });
        tablaPacientes.getSelectionModel().selectedItemProperty().addListener((obs, viejo, nuevo) ->
            mensajeBtn.setDisable(nuevo == null || servicio == null));

        Button refrescarBtn = new Button("Actualizar");
        refrescarBtn.getStyleClass().add("boton-secundario");
        refrescarBtn.setOnAction(e -> refrescarPacientes());

        TextField buscarField = new TextField();
        buscarField.setPromptText("Buscar paciente...");
        buscarField.setPrefWidth(200);
        buscarField.textProperty().addListener((obs, viejo, nuevo) -> {
            String filtro = nuevo == null ? "" : nuevo.trim().toLowerCase();
            datosPacientesFiltrados.setPredicate(p -> filtro.isEmpty()
                || p.getNombre().toLowerCase().contains(filtro));
        });

        HBox barra = new HBox(10, buscarField, ayuda, espaciador(), mensajeBtn, refrescarBtn);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(14, 24, 10, 24));

        return new VBox(barra, tablaPacientes);
    }

    private void mostrarDialogoMensaje(Paciente paciente) {
        Stage dialogo = new Stage();
        dialogo.initOwner(tablaPacientes.getScene().getWindow());
        dialogo.initModality(Modality.WINDOW_MODAL);
        dialogo.setTitle("Mensaje para " + paciente.getNombre());

        Label titulo = new Label("Mensaje para " + paciente.getNombre());
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14.5px; -fx-text-fill: #0f172a;");

        Label etiquetaCanal = new Label("Enviar por:");
        ComboBox<CanalNotificacion> canalCombo = new ComboBox<>(FXCollections.observableArrayList(CanalNotificacion.values()));
        canalCombo.setValue(paciente.getCanalPreferido());
        HBox filaCanal = new HBox(8, etiquetaCanal, canalCombo);
        filaCanal.setAlignment(Pos.CENTER_LEFT);

        TextArea texto = new TextArea();
        texto.setPromptText("Escribí el mensaje para el paciente...");
        texto.setWrapText(true);
        texto.setPrefRowCount(6);

        Label estado = new Label();
        estado.setWrapText(true);

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.getStyleClass().add("boton-secundario");
        cancelarBtn.setOnAction(e -> dialogo.close());

        Button enviarBtn = new Button("Enviar");
        enviarBtn.getStyleClass().add("boton-primario");
        enviarBtn.setOnAction(e -> {
            String mensaje = texto.getText().trim();
            if (mensaje.isEmpty() || servicio == null) {
                return;
            }
            enviarBtn.setDisable(true);
            estado.getStyleClass().removeAll("estado-ok", "estado-error");
            estado.setText("Enviando...");
            CanalNotificacion canal = canalCombo.getValue();
            ejecutarEnFondo(
                () -> { servicio.enviarMensajeManual(paciente, canal, mensaje); return null; },
                v -> {
                    estado.setText("Mensaje enviado.");
                    estado.getStyleClass().add("estado-ok");
                    enviarBtn.setDisable(false);
                },
                error -> {
                    estado.setText("Error: " + causaRaiz(error));
                    estado.getStyleClass().add("estado-error");
                    enviarBtn.setDisable(false);
                }
            );
        });

        HBox botones = new HBox(10, cancelarBtn, enviarBtn);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12, titulo, filaCanal, texto, estado, botones);
        layout.setPadding(new Insets(22));
        layout.setPrefWidth(440);
        layout.setStyle("-fx-background-color: white;");

        Scene escena = new Scene(layout);
        escena.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
        dialogo.setScene(escena);
        dialogo.show();
    }

    private void refrescarPacientes() {
        if (servicio == null) {
            return;
        }
        ejecutarEnFondo(
            () -> servicio.listarPacientes(),
            datosPacientes::setAll,
            error -> mostrarError("No se pudo actualizar la lista de pacientes", error)
        );
    }

    private void actualizarCanalPaciente(Long pacienteId, CanalNotificacion canal) {
        if (servicio == null) {
            return;
        }
        ejecutarEnFondo(
            () -> { servicio.actualizarCanalPreferido(pacienteId, canal); return null; },
            v -> refrescarPacientes(),
            error -> { mostrarError("No se pudo actualizar el canal preferido", error); refrescarPacientes(); }
        );
    }

    // ---------- Pestaña Mensaje ----------

    private Node crearPestañaMensaje() {
        Label titulo = new Label("Mensaje del recordatorio automático");
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #0f172a;");

        Label ayuda = new Label(
            "Este es el texto que reciben los pacientes en el recordatorio automático "
            + "(email, SMS o WhatsApp — los tres usan el mismo mensaje). Podés usar estos datos del turno:");
        ayuda.setWrapText(true);
        ayuda.setStyle("-fx-text-fill: #64748b;");

        Label placeholders = new Label("{paciente}   {profesional}   {especialidad}   {fecha}   {hora}");
        placeholders.setStyle("-fx-font-family: monospace; -fx-text-fill: #0f766e; -fx-font-weight: bold;");
        placeholders.setWrapText(true);

        plantillaMensajeArea = new TextArea();
        plantillaMensajeArea.setWrapText(true);
        plantillaMensajeArea.setPrefRowCount(6);
        plantillaMensajeArea.textProperty().addListener((obs, viejo, nuevo) -> actualizarVistaPrevia());

        Label tituloPreview = new Label("Así lo va a ver el paciente:");
        tituloPreview.setStyle("-fx-font-weight: bold; -fx-padding: 8 0 0 0;");

        vistaPreviaLabel = new Label();
        vistaPreviaLabel.setWrapText(true);
        vistaPreviaLabel.setStyle(
            "-fx-background-color: #f1f5f4; -fx-padding: 14; -fx-background-radius: 8; -fx-text-fill: #0f172a;");

        Button restaurarBtn = new Button("Restaurar mensaje por defecto");
        restaurarBtn.getStyleClass().add("boton-secundario");
        restaurarBtn.setOnAction(e -> plantillaMensajeArea.setText(PlantillaMensaje.PLANTILLA_POR_DEFECTO));

        Button guardarMensajeBtn = new Button("Guardar mensaje");
        guardarMensajeBtn.getStyleClass().add("boton-primario");
        guardarMensajeBtn.setOnAction(e -> guardarTodo());

        HBox botones = new HBox(10, restaurarBtn, guardarMensajeBtn);
        botones.setPadding(new Insets(6, 0, 0, 0));

        VBox layout = new VBox(10, titulo, ayuda, placeholders, plantillaMensajeArea,
            tituloPreview, vistaPreviaLabel, botones);
        layout.setPadding(new Insets(24));
        layout.setMaxWidth(680);

        ScrollPane scroll = new ScrollPane(layout);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private void actualizarVistaPrevia() {
        String ejemplo = plantillaMensajeArea.getText()
            .replace("{paciente}", "Juan Pérez")
            .replace("{profesional}", "Dr. Fernández")
            .replace("{especialidad}", "Kinesiología")
            .replace("{fecha}", "28/08/2026")
            .replace("{hora}", "14:30");
        vistaPreviaLabel.setText(ejemplo);
    }

    // ---------- Pestaña Configuración ----------

    private Node crearPestañaConfiguracion() {
        dbUrl = new TextField();
        dbUsuario = new TextField();
        dbPassword = new PasswordField();
        smtpHost = new TextField();
        smtpUsuario = new TextField();
        smtpPassword = new PasswordField();
        twilioSid = new TextField();
        twilioToken = new PasswordField();
        numeroSms = new TextField();
        numeroWhatsApp = new TextField();
        horasAnticipacion = new TextField();
        intervaloMinutos = new TextField();

        GridPane grid = new GridPane();
        grid.getStyleClass().add("contenedor-config");

        int fila = 0;
        fila = agregarSeccion(grid, fila, "Sistema de turnos (solo lectura)");
        Label notaSoloLectura = new Label(
            "Esta app nunca escribe acá — solo lee pacientes y turnos para copiarlos a su base propia.");
        notaSoloLectura.setWrapText(true);
        notaSoloLectura.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11.5px;");
        grid.add(notaSoloLectura, 0, fila, 2, 1);
        fila++;
        fila = agregarCampo(grid, fila, "URL JDBC", dbUrl);
        fila = agregarCampo(grid, fila, "Usuario", dbUsuario);
        fila = agregarCampo(grid, fila, "Contraseña", dbPassword);

        fila = agregarSeccion(grid, fila, "Email (SMTP)");
        fila = agregarCampo(grid, fila, "Servidor SMTP", smtpHost);
        fila = agregarCampo(grid, fila, "Usuario / email", smtpUsuario);
        fila = agregarCampo(grid, fila, "Contraseña de aplicación", smtpPassword);

        fila = agregarSeccion(grid, fila, "SMS y WhatsApp (Twilio) — opcional");
        fila = agregarCampo(grid, fila, "Account SID", twilioSid);
        fila = agregarCampo(grid, fila, "Auth Token", twilioToken);
        fila = agregarCampo(grid, fila, "Número para SMS", numeroSms);
        fila = agregarCampo(grid, fila, "Número para WhatsApp", numeroWhatsApp);

        fila = agregarSeccion(grid, fila, "Recordatorios");
        fila = agregarCampo(grid, fila, "Horas de anticipación", horasAnticipacion);
        fila = agregarCampo(grid, fila, "Intervalo de chequeo (minutos)", intervaloMinutos);

        fila = agregarSeccion(grid, fila, "Arranque");
        inicioAutomaticoCheck = new CheckBox("Iniciar automáticamente al encender la PC");
        boolean autoInicioDisponible = InicioAutomatico.esSoportado() && InicioAutomatico.esEjecutableEmpaquetado();
        inicioAutomaticoCheck.setDisable(!autoInicioDisponible);
        inicioAutomaticoCheck.setTooltip(new Tooltip(!InicioAutomatico.esSoportado()
            ? "Solo disponible en Windows"
            : !autoInicioDisponible
                ? "Solo disponible en la versión instalada (no en modo desarrollo)"
                : "Agrega/quita un acceso directo en el inicio de sesión de Windows"));
        inicioAutomaticoCheck.setOnAction(e -> alternarInicioAutomatico(inicioAutomaticoCheck.isSelected()));
        grid.add(inicioAutomaticoCheck, 0, fila, 2, 1);
        fila++;

        Button probarConexionBtn = new Button("Probar conexión al sistema de turnos");
        probarConexionBtn.getStyleClass().add("boton-secundario");
        probarConexionBtn.setOnAction(e -> probarConexion());

        Button probarEnvioBtn = new Button("Enviar mensaje de prueba");
        probarEnvioBtn.getStyleClass().add("boton-secundario");
        probarEnvioBtn.setOnAction(e -> mostrarDialogoPrueba());

        Button guardarBtn = new Button("Guardar y (re)iniciar el servicio");
        guardarBtn.getStyleClass().add("boton-primario");
        guardarBtn.setOnAction(e -> guardarTodo());

        HBox botones = new HBox(10, probarConexionBtn, probarEnvioBtn, guardarBtn);
        grid.add(botones, 0, fila, 2, 1);
        fila++;

        configEstadoLabel = new Label();
        configEstadoLabel.setWrapText(true);
        grid.add(configEstadoLabel, 0, fila, 2, 1);

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        return scroll;
    }

    public void cargarConfiguracion(Properties props) {
        dbUrl.setText(props.getProperty("origen.db.url", ""));
        dbUsuario.setText(props.getProperty("origen.db.usuario", ""));
        dbPassword.setText(props.getProperty("origen.db.password", ""));
        smtpHost.setText(props.getProperty("smtp.host", "smtp.gmail.com"));
        smtpUsuario.setText(props.getProperty("smtp.usuario", ""));
        smtpPassword.setText(props.getProperty("smtp.password", ""));
        twilioSid.setText(props.getProperty("twilio.accountSid", ""));
        twilioToken.setText(props.getProperty("twilio.authToken", ""));
        numeroSms.setText(props.getProperty("twilio.numeroSms", ""));
        numeroWhatsApp.setText(props.getProperty("twilio.numeroWhatsApp", ""));
        horasAnticipacion.setText(props.getProperty("recordatorio.horasAnticipacion", "24"));
        intervaloMinutos.setText(props.getProperty("recordatorio.intervaloMinutos", "15"));
        plantillaMensajeArea.setText(props.getProperty("mensaje.plantilla", PlantillaMensaje.PLANTILLA_POR_DEFECTO));
        actualizarVistaPrevia();

        if (!inicioAutomaticoCheck.isDisable()) {
            inicioAutomaticoCheck.setSelected(InicioAutomatico.estaActivo());
        }
    }

    /** Junta el estado actual de TODOS los campos (Configuración + Mensaje), sin importar desde qué pestaña se guarda. */
    private Properties construirPropiedadesActuales() {
        Properties props = new Properties();
        props.setProperty("origen.db.url", dbUrl.getText().trim());
        props.setProperty("origen.db.usuario", dbUsuario.getText().trim());
        props.setProperty("origen.db.password", dbPassword.getText());
        props.setProperty("smtp.host", smtpHost.getText().trim());
        props.setProperty("smtp.usuario", smtpUsuario.getText().trim());
        props.setProperty("smtp.password", smtpPassword.getText());
        props.setProperty("twilio.accountSid", twilioSid.getText().trim());
        props.setProperty("twilio.authToken", twilioToken.getText().trim());
        props.setProperty("twilio.numeroSms", numeroSms.getText().trim());
        props.setProperty("twilio.numeroWhatsApp", numeroWhatsApp.getText().trim());
        props.setProperty("recordatorio.horasAnticipacion", horasAnticipacion.getText().trim());
        props.setProperty("recordatorio.intervaloMinutos", intervaloMinutos.getText().trim());
        props.setProperty("mensaje.plantilla", plantillaMensajeArea.getText());
        return props;
    }

    private void guardarTodo() {
        String errorValidacion = validarCamposNumericos();
        if (errorValidacion != null) {
            mostrarInfo("Revisá la configuración", errorValidacion);
            return;
        }
        app.guardarYReiniciar(construirPropiedadesActuales());
    }

    /** null si está todo bien; si no, el mensaje para mostrarle al usuario. */
    private String validarCamposNumericos() {
        Integer horas = enteroPositivoOrNull(horasAnticipacion.getText());
        if (horas == null) {
            return "\"Horas de anticipación\" tiene que ser un número entero mayor a cero.";
        }
        Integer intervalo = enteroPositivoOrNull(intervaloMinutos.getText());
        if (intervalo == null) {
            return "\"Intervalo de chequeo\" tiene que ser un número entero mayor a cero (en minutos).";
        }
        return null;
    }

    private Integer enteroPositivoOrNull(String texto) {
        try {
            int valor = Integer.parseInt(texto.trim());
            return valor > 0 ? valor : null;
        } catch (Exception e) {
            return null;
        }
    }

    private void alternarInicioAutomatico(boolean activar) {
        ejecutarEnFondo(
            () -> {
                if (activar) {
                    InicioAutomatico.activar();
                } else {
                    InicioAutomatico.desactivar();
                }
                return null;
            },
            v -> { /* nada más que hacer */ },
            error -> {
                inicioAutomaticoCheck.setSelected(!activar);
                mostrarError("No se pudo cambiar el inicio automático", error);
            }
        );
    }

    private void mostrarDialogoPrueba() {
        Stage dialogo = new Stage();
        dialogo.initOwner(dbUrl.getScene().getWindow());
        dialogo.initModality(Modality.WINDOW_MODAL);
        dialogo.setTitle("Mensaje de prueba");

        Label titulo = new Label("Enviar mensaje de prueba");
        titulo.setStyle("-fx-font-weight: bold; -fx-font-size: 14.5px; -fx-text-fill: #0f172a;");

        Label ayuda = new Label("Usa los datos tal como están en el formulario, aunque todavía no los hayas guardado.");
        ayuda.setWrapText(true);
        ayuda.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11.5px;");

        ComboBox<CanalNotificacion> canalCombo = new ComboBox<>(FXCollections.observableArrayList(CanalNotificacion.values()));
        canalCombo.setValue(CanalNotificacion.EMAIL);

        TextField destinoField = new TextField();
        destinoField.setPromptText("Email o teléfono (+549...)");

        Label estado = new Label();
        estado.setWrapText(true);

        Button cancelarBtn = new Button("Cancelar");
        cancelarBtn.getStyleClass().add("boton-secundario");
        cancelarBtn.setOnAction(e -> dialogo.close());

        Button enviarBtn = new Button("Enviar");
        enviarBtn.getStyleClass().add("boton-primario");
        enviarBtn.setOnAction(e -> {
            String destino = destinoField.getText().trim();
            if (destino.isEmpty()) {
                return;
            }
            enviarBtn.setDisable(true);
            estado.getStyleClass().removeAll("estado-ok", "estado-error");
            estado.setText("Enviando...");
            CanalNotificacion canal = canalCombo.getValue();
            Properties props = construirPropiedadesActuales();
            ejecutarEnFondo(
                () -> { app.probarEnvio(canal, destino, props); return null; },
                v -> {
                    estado.setText("Mensaje enviado.");
                    estado.getStyleClass().add("estado-ok");
                    enviarBtn.setDisable(false);
                },
                error -> {
                    estado.setText("Error: " + causaRaiz(error));
                    estado.getStyleClass().add("estado-error");
                    enviarBtn.setDisable(false);
                }
            );
        });

        HBox botones = new HBox(10, cancelarBtn, enviarBtn);
        botones.setAlignment(Pos.CENTER_RIGHT);

        VBox layout = new VBox(12, titulo, ayuda, new Label("Canal:"), canalCombo, destinoField, estado, botones);
        layout.setPadding(new Insets(22));
        layout.setPrefWidth(420);
        layout.setStyle("-fx-background-color: white;");

        Scene escena = new Scene(layout);
        escena.getStylesheets().add(getClass().getResource("/gui/styles.css").toExternalForm());
        dialogo.setScene(escena);
        dialogo.show();
    }

    private void probarConexion() {
        String url = dbUrl.getText().trim();
        String usuario = dbUsuario.getText().trim();
        String password = dbPassword.getText();

        configEstadoLabel.getStyleClass().removeAll("estado-ok", "estado-error");
        configEstadoLabel.setText("Probando conexión...");

        ejecutarEnFondo(
            () -> {
                try (Connection c = DriverManager.getConnection(url, usuario, password)) {
                    return true;
                }
            },
            ok -> {
                configEstadoLabel.setText("Conexión exitosa.");
                configEstadoLabel.getStyleClass().add("estado-ok");
            },
            error -> {
                configEstadoLabel.setText("Error de conexión: " + causaRaiz(error));
                configEstadoLabel.getStyleClass().add("estado-error");
            }
        );
    }

    // ---------- Utilidades ----------

    private int agregarSeccion(GridPane grid, int fila, String titulo) {
        Label label = new Label(titulo);
        label.getStyleClass().add("seccion-titulo");
        grid.add(label, 0, fila, 2, 1);
        return fila + 1;
    }

    private int agregarCampo(GridPane grid, int fila, String etiqueta, Control campo) {
        grid.add(new Label(etiqueta), 0, fila);
        campo.setPrefWidth(320);
        grid.add(campo, 1, fila);
        return fila + 1;
    }

    private Label etiqueta(String texto, String estilo) {
        Label label = new Label(texto);
        if (estilo != null) {
            label.getStyleClass().add(estilo);
        }
        return label;
    }

    private Region espaciador() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }

    private int parseIntSeguro(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (Exception e) {
            return porDefecto;
        }
    }

    private String causaRaiz(Throwable t) {
        Throwable actual = t;
        while (actual.getCause() != null) {
            actual = actual.getCause();
        }
        String mensaje = actual.getMessage() != null ? actual.getMessage() : actual.toString();
        return MensajesAmigables.traducir(mensaje);
    }

    private void mostrarAcercaDe() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Acerca de");
        alert.setHeaderText("Recordatorio de turnos - FKT Lomas");
        alert.setContentText(
            "Versión " + VERSION + "\n"
            + "Centraliza el envío de recordatorios de turno por email, SMS y WhatsApp.\n\n"
            + "Log: ./logs/app.log\n"
            + "Base local: ./data/recordatorios.mv.db\n"
            + "Respaldos: ./data/backups/");
        alert.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(causaRaiz(error));
        alert.showAndWait();
    }

    private <T> void ejecutarEnFondo(Callable<T> trabajo, Consumer<T> alExito, Consumer<Throwable> alError) {
        Thread hilo = new Thread(() -> {
            try {
                T resultado = trabajo.call();
                javafx.application.Platform.runLater(() -> alExito.accept(resultado));
            } catch (Throwable e) {
                javafx.application.Platform.runLater(() -> alError.accept(e));
            }
        });
        hilo.setDaemon(true);
        hilo.start();
    }
}
