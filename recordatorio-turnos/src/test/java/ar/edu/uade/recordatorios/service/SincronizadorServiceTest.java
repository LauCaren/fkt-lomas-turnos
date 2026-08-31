package ar.edu.uade.recordatorios.service;

import ar.edu.uade.recordatorios.config.LocalSchemaInitializer;
import ar.edu.uade.recordatorios.repository.OrigenRepository;
import ar.edu.uade.recordatorios.sync.EstadoSincronizacion;
import ar.edu.uade.recordatorios.sync.PacienteOrigen;
import ar.edu.uade.recordatorios.sync.TurnoOrigen;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Contra una base H2 en memoria de verdad (no mockeada): lo que hay que
 * verificar acá es el comportamiento real del MERGE/baja lógica, que sería
 * poco útil reimplementar con mocks de Connection/PreparedStatement.
 */
@ExtendWith(MockitoExtension.class)
class SincronizadorServiceTest {

    @Mock private OrigenRepository origenRepository;

    private DataSource localDataSource;
    private SincronizadorService service;

    @BeforeEach
    void setUp() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:sync-test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        localDataSource = new HikariDataSource(config);
        LocalSchemaInitializer.inicializar(localDataSource);

        service = new SincronizadorService(origenRepository, localDataSource);
    }

    @AfterEach
    void tearDown() {
        ((HikariDataSource) localDataSource).close();
    }

    @Test
    void primeraSincronizacionInsertaPacienteConCanalPorDefecto() throws Exception {
        when(origenRepository.listarPacientes()).thenReturn(
            List.of(new PacienteOrigen(1L, "Juan Pérez", "+5491100000000", "juan@test.com")));
        when(origenRepository.listarTurnosDesde(any())).thenReturn(List.of());

        EstadoSincronizacion resultado = service.sincronizar();

        assertThat(resultado.fueExitosa()).isTrue();
        assertThat(resultado.pacientesSincronizados()).isEqualTo(1);

        try (Connection conn = localDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nombre, canal_preferido, activo FROM pacientes WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("nombre")).isEqualTo("Juan Pérez");
            assertThat(rs.getString("canal_preferido")).isEqualTo("EMAIL");
            assertThat(rs.getBoolean("activo")).isTrue();
        }
    }

    @Test
    void resincronizarNoPisaElCanalPreferidoElegidoEnLaApp() throws Exception {
        when(origenRepository.listarPacientes()).thenReturn(
            List.of(new PacienteOrigen(1L, "Juan Pérez", "+5491100000000", "juan@test.com")));
        when(origenRepository.listarTurnosDesde(any())).thenReturn(List.of());

        service.sincronizar(); // primera vez: queda en EMAIL (default)

        try (Connection conn = localDataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("UPDATE pacientes SET canal_preferido = 'SMS' WHERE id = 1"); // lo cambia el staff desde la GUI
        }

        // El origen ahora informa un teléfono distinto: la sincronización debe
        // actualizar ese dato, pero no tocar el canal que ya eligió el staff.
        when(origenRepository.listarPacientes()).thenReturn(
            List.of(new PacienteOrigen(1L, "Juan Pérez", "+5491199999999", "juan@test.com")));

        service.sincronizar();

        try (Connection conn = localDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT telefono, canal_preferido FROM pacientes WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("telefono")).isEqualTo("+5491199999999");
            assertThat(rs.getString("canal_preferido")).isEqualTo("SMS");
        }
    }

    @Test
    void pacienteQueDejaDeApareceEnElOrigenQuedaInactivo() throws Exception {
        when(origenRepository.listarPacientes()).thenReturn(
            List.of(new PacienteOrigen(1L, "Juan Pérez", "+5491100000000", "juan@test.com")));
        when(origenRepository.listarTurnosDesde(any())).thenReturn(List.of());
        service.sincronizar();

        when(origenRepository.listarPacientes()).thenReturn(List.of()); // ya no está en el origen
        service.sincronizar();

        try (Connection conn = localDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT activo FROM pacientes WHERE id = 1")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getBoolean("activo")).isFalse();
        }
    }

    @Test
    void turnoSeSincronizaCorrectamente() throws Exception {
        when(origenRepository.listarPacientes()).thenReturn(
            List.of(new PacienteOrigen(1L, "Juan Pérez", "+5491100000000", "juan@test.com")));
        when(origenRepository.listarTurnosDesde(any())).thenReturn(List.of(
            new TurnoOrigen(100L, 1L, LocalDateTime.now().plusHours(3), "Dr. Fernández", "Kinesiología", "Consultorio 1")));

        EstadoSincronizacion resultado = service.sincronizar();

        assertThat(resultado.turnosSincronizados()).isEqualTo(1);
        try (Connection conn = localDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT profesional, especialidad FROM turnos WHERE id = 100")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("profesional")).isEqualTo("Dr. Fernández");
            assertThat(rs.getString("especialidad")).isEqualTo("Kinesiología");
        }
    }

    @Test
    void fallaDeConexionAlOrigenNoRompeYQuedaRegistradaComoError() {
        when(origenRepository.listarPacientes()).thenThrow(new RuntimeException("No se pudo conectar al origen"));

        EstadoSincronizacion resultado = service.sincronizar();

        assertThat(resultado.fueExitosa()).isFalse();
        assertThat(resultado.detalleError()).contains("No se pudo conectar");

        EstadoSincronizacion guardado = service.obtenerUltimoEstado();
        assertThat(guardado.fueExitosa()).isFalse();
    }
}
