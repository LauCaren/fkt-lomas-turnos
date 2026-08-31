package ar.edu.uade.recordatorios.service;

import ar.edu.uade.recordatorios.config.AppPaths;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Respaldo diario de la base local: pacientes, turnos, canal preferido y el
 * historial de recordatorios están solo acá (la base de origen no los
 * tiene), así que si se pierde este archivo se pierde esa información.
 * Usa el comando BACKUP propio de H2, que genera una copia consistente
 * aunque la base esté en uso — copiar el archivo .mv.db a mano mientras la
 * app está corriendo no garantiza eso.
 */
public class RespaldoService {

    private static final Logger log = Logger.getLogger(RespaldoService.class.getName());
    private static final int MAX_RESPALDOS = 7;
    private static final Path CARPETA_RESPALDOS = AppPaths.directorioBase().resolve("data").resolve("backups");

    private RespaldoService() {
    }

    /** No hace nada si ya se respaldó hoy. */
    public static void respaldarSiCorresponde(DataSource localDataSource) {
        try {
            Files.createDirectories(CARPETA_RESPALDOS);
            String marca = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Path destino = CARPETA_RESPALDOS.resolve("recordatorios-" + marca + ".zip");

            if (Files.exists(destino)) {
                return;
            }

            try (Connection conn = localDataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("BACKUP TO '" + destino.toString().replace("'", "''") + "'");
            }

            log.info(() -> "Respaldo de la base local creado: " + destino);
            limpiarRespaldosViejos();

        } catch (SQLException | IOException e) {
            log.log(Level.WARNING, "No se pudo respaldar la base local", e);
        }
    }

    private static void limpiarRespaldosViejos() throws IOException {
        try (Stream<Path> stream = Files.list(CARPETA_RESPALDOS)) {
            List<Path> archivos = stream
                .sorted(Comparator.comparing(Path::getFileName, Comparator.reverseOrder()))
                .toList();
            for (int i = MAX_RESPALDOS; i < archivos.size(); i++) {
                Files.deleteIfExists(archivos.get(i));
            }
        }
    }
}
