package ar.edu.uade.recordatorios.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/** Conexión de SOLO LECTURA a la base del sistema de turnos real, fkt-turnos-backend (PostgreSQL) — ver OrigenRepository. */
public class DatabaseConfig {

    public static DataSource crearDataSource(String url, String usuario, String password) {
        HikariConfig hikariConfig = new HikariConfig();
        // Explícito por la misma razón que en LocalDatabaseConfig: el jar
        // empaquetado trae más de un driver JDBC y el auto-registro por
        // ServiceLoader puede perder uno de los dos.
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(usuario);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setConnectionTimeout(10_000);
        return new HikariDataSource(hikariConfig);
    }
}
