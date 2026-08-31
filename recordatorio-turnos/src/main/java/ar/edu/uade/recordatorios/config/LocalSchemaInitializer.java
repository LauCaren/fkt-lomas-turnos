package ar.edu.uade.recordatorios.config;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Crea las tablas de la base local (H2) si todavía no existen. Idempotente: seguro de llamar en cada arranque. */
public class LocalSchemaInitializer {

    public static void inicializar(DataSource dataSource) {
        String sql = leerRecurso("/sql/schema-local.sql");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sentencia : sql.split(";")) {
                String limpia = sentencia.strip();
                if (!limpia.isEmpty()) {
                    stmt.execute(limpia);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error inicializando la base local", e);
        }
    }

    private static String leerRecurso(String ruta) {
        try (InputStream in = LocalSchemaInitializer.class.getResourceAsStream(ruta)) {
            if (in == null) {
                throw new RuntimeException("No se encontró el recurso " + ruta);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Error leyendo " + ruta, e);
        }
    }
}
