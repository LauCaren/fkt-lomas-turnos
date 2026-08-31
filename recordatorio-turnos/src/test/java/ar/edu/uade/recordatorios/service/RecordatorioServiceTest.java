package ar.edu.uade.recordatorios.service;

import ar.edu.uade.recordatorios.model.CanalNotificacion;
import ar.edu.uade.recordatorios.model.EstadoRecordatorio;
import ar.edu.uade.recordatorios.model.Paciente;
import ar.edu.uade.recordatorios.model.Recordatorio;
import ar.edu.uade.recordatorios.model.Turno;
import ar.edu.uade.recordatorios.notification.NotificacionException;
import ar.edu.uade.recordatorios.notification.Notificador;
import ar.edu.uade.recordatorios.notification.NotificadorFactory;
import ar.edu.uade.recordatorios.repository.PacienteRepository;
import ar.edu.uade.recordatorios.repository.RecordatorioRepository;
import ar.edu.uade.recordatorios.repository.TurnoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioServiceTest {

    @Mock private TurnoRepository turnoRepository;
    @Mock private RecordatorioRepository recordatorioRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private NotificadorFactory notificadorFactory;
    @Mock private SincronizadorService sincronizadorService;
    @Mock private Notificador notificador;

    private RecordatorioService service;

    @BeforeEach
    void setUp() {
        service = new RecordatorioService(
            turnoRepository, recordatorioRepository, pacienteRepository, notificadorFactory, sincronizadorService);
    }

    private Turno turnoDePrueba() {
        Paciente paciente = new Paciente(1L, "Juan Pérez", "+5491100000000", "juan@test.com", CanalNotificacion.EMAIL);
        return new Turno(10L, paciente, LocalDateTime.now().plusHours(2), "Dr. Fernández", "Kinesiología", "Consultorio 1");
    }

    @Test
    void sincronizaAntesDeChequear() {
        when(turnoRepository.buscarTurnosProximos(any(), any())).thenReturn(List.of());

        service.procesarRecordatoriosPendientes(24);

        verify(sincronizadorService, times(1)).sincronizar();
    }

    @Test
    void turnoNuevoCreaRecordatorioYLoEnviaConExito() throws NotificacionException {
        Turno turno = turnoDePrueba();
        when(turnoRepository.buscarTurnosProximos(any(), any())).thenReturn(List.of(turno));
        when(recordatorioRepository.buscarPorTurno(turno.getId())).thenReturn(Optional.empty());
        when(recordatorioRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificadorFactory.obtener(CanalNotificacion.EMAIL)).thenReturn(notificador);

        service.procesarRecordatoriosPendientes(24);

        ArgumentCaptor<Recordatorio> captor = ArgumentCaptor.forClass(Recordatorio.class);
        verify(recordatorioRepository).actualizarEstado(captor.capture());
        Recordatorio actualizado = captor.getValue();

        assertThat(actualizado.getEstado()).isEqualTo(EstadoRecordatorio.ENVIADO);
        assertThat(actualizado.getFechaEnvio()).isNotNull();
        verify(notificador, times(1)).enviar(turno);
    }

    @Test
    void turnoConRecordatorioPendienteExistenteNoCreaUnoNuevo() throws NotificacionException {
        Turno turno = turnoDePrueba();
        Recordatorio existente = new Recordatorio(turno, CanalNotificacion.EMAIL);
        existente.setId(99L);

        when(turnoRepository.buscarTurnosProximos(any(), any())).thenReturn(List.of(turno));
        when(recordatorioRepository.buscarPorTurno(turno.getId())).thenReturn(Optional.of(existente));
        when(notificadorFactory.obtener(CanalNotificacion.EMAIL)).thenReturn(notificador);

        service.procesarRecordatoriosPendientes(24);

        // No debe insertar un recordatorio nuevo: ya había uno pendiente para ese turno.
        verify(recordatorioRepository, never()).guardar(any());
        verify(recordatorioRepository).actualizarEstado(existente);
    }

    @Test
    void fallaAlEnviarDejaElRecordatorioPendienteConUnIntento() throws NotificacionException {
        Turno turno = turnoDePrueba();
        when(turnoRepository.buscarTurnosProximos(any(), any())).thenReturn(List.of(turno));
        when(recordatorioRepository.buscarPorTurno(turno.getId())).thenReturn(Optional.empty());
        when(recordatorioRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificadorFactory.obtener(CanalNotificacion.EMAIL)).thenReturn(notificador);
        doThrow(new NotificacionException("SMTP caído", new RuntimeException())).when(notificador).enviar(turno);

        service.procesarRecordatoriosPendientes(24);

        ArgumentCaptor<Recordatorio> captor = ArgumentCaptor.forClass(Recordatorio.class);
        verify(recordatorioRepository).actualizarEstado(captor.capture());
        Recordatorio actualizado = captor.getValue();

        assertThat(actualizado.getEstado()).isEqualTo(EstadoRecordatorio.PENDIENTE);
        assertThat(actualizado.getIntentos()).isEqualTo(1);
        assertThat(actualizado.getDetalleError()).isEqualTo("SMTP caído");
    }

    @Test
    void tercerFalloConsecutivoMarcaFallido() throws NotificacionException {
        Turno turno = turnoDePrueba();
        Recordatorio recordatorio = new Recordatorio(turno, CanalNotificacion.EMAIL);
        recordatorio.setId(5L);
        recordatorio.setIntentos(2); // ya falló dos veces antes

        when(recordatorioRepository.buscarPorId(5L)).thenReturn(Optional.of(recordatorio));
        when(notificadorFactory.obtener(CanalNotificacion.EMAIL)).thenReturn(notificador);
        doThrow(new NotificacionException("Timeout", new RuntimeException())).when(notificador).enviar(turno);

        service.reintentarRecordatorio(5L);

        assertThat(recordatorio.getEstado()).isEqualTo(EstadoRecordatorio.FALLIDO);
        assertThat(recordatorio.getIntentos()).isEqualTo(3);
    }

    @Test
    void canalSinConfigurarNoRompeElProcesamientoDelResto() throws NotificacionException {
        Turno turno = turnoDePrueba();
        when(turnoRepository.buscarTurnosProximos(any(), any())).thenReturn(List.of(turno));
        when(recordatorioRepository.buscarPorTurno(turno.getId())).thenReturn(Optional.empty());
        when(recordatorioRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notificadorFactory.obtener(CanalNotificacion.EMAIL))
            .thenThrow(new IllegalArgumentException("El canal EMAIL no está configurado"));

        // No debería tirar la excepción hacia afuera (rompería el scheduler para siempre).
        service.procesarRecordatoriosPendientes(24);

        ArgumentCaptor<Recordatorio> captor = ArgumentCaptor.forClass(Recordatorio.class);
        verify(recordatorioRepository).actualizarEstado(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoRecordatorio.PENDIENTE);
        assertThat(captor.getValue().getDetalleError()).contains("no está configurado");
    }

    @Test
    void reintentarRecordatorioInexistenteLanzaExcepcion() {
        when(recordatorioRepository.buscarPorId(404L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.reintentarRecordatorio(404L));
    }

    @Test
    void enviarMensajeManualDelegaEnElNotificadorDelCanalElegido() throws NotificacionException {
        Paciente paciente = new Paciente(2L, "Ana Gómez", "+5491111111111", "ana@test.com", CanalNotificacion.EMAIL);
        when(notificadorFactory.obtener(CanalNotificacion.WHATSAPP)).thenReturn(notificador);

        service.enviarMensajeManual(paciente, CanalNotificacion.WHATSAPP, "Hola, te esperamos mañana.");

        verify(notificador).enviarMensaje(paciente, "Hola, te esperamos mañana.");
    }

    @Test
    void actualizarCanalPreferidoDelegaEnElRepositorio() {
        service.actualizarCanalPreferido(7L, CanalNotificacion.SMS);

        verify(pacienteRepository).actualizarCanalPreferido(7L, CanalNotificacion.SMS);
    }
}
