package ar.edu.uade.recordatorios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Base propia de la app (H2 embebido, un archivo local). Acá vive el espejo
 * de pacientes/turnos y el canal preferido de cada paciente — nunca la base
 * del sistema de turnos real, que solo se lee (ver DatabaseConfig +
 * OrigenRepository). No requiere instalar ni configurar nada: el archivo se
 * crea solo la primera vez que arranca la app, en data/recordatorios dentro
 * de {@link AppPaths#directorioBase()}.
 */
public class LocalDatabaseConfig {

    public static DataSource crearDataSource() {
        HikariConfig config = new HikariConfig();
        // Explícito en vez de depender del auto-registro por ServiceLoader:
        // el jar empaquetado trae más de un driver JDBC (MySQL y H2), y el
        // plugin de empaquetado puede pisar el archivo de registro de uno
        // con el del otro (los dos declaran el mismo META-INF/services).
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:file:" + AppPaths.directorioBase().resolve("data").resolve("recordatorios"));
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        return new HikariDataSource(config);
    }
}
