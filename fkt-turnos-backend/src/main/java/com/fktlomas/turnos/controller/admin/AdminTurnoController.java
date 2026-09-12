package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.dto.RecordatorioDto;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.admin.ActualizarAsistenciaRequest;
import com.fktlomas.turnos.dto.admin.AsignarPacienteRequest;
import com.fktlomas.turnos.dto.admin.ReprogramarTurnoRequest;
import com.fktlomas.turnos.dto.admin.ReprogramarTurnoResponse;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.dto.auditoria.RegistroAuditoriaDto;
import com.fktlomas.turnos.security.AppUserDetails;
import com.fktlomas.turnos.service.AuditoriaService;
import com.fktlomas.turnos.service.RecordatorioService;
import com.fktlomas.turnos.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/turnos")
@RequiredArgsConstructor
public class AdminTurnoController {

    private final TurnoService turnoService;
    private final AuditoriaService auditoriaService;
    private final RecordatorioService recordatorioService;

    @GetMapping
    public List<TurnoAdminDto> agenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) Long especialidadId) {
        return turnoService.agenda(desde, hasta, medicoId, especialidadId);
    }

    private static final int TAMANIO_PAGINA_MAXIMO = 200;

    @GetMapping("/buscar")
    public Page<TurnoAdminDto> buscar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Long medicoId,
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) EstadoTurno estado,
            @PageableDefault(size = TAMANIO_PAGINA_MAXIMO) Pageable pageable) {
        // Sin Sort acá a propósito: la propia @Query de buscarTurnos ya trae su ORDER BY
        // (fecha, horaInicio) — pasarle además un Sort de Pageable duplicaría la cláusula.
        Pageable pageableAcotado = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), TAMANIO_PAGINA_MAXIMO));
        return turnoService.buscarTurnos(desde, hasta, medicoId, especialidadId, estado, pageableAcotado);
    }

    @PostMapping("/{id}/reservar-para-paciente")
    public TurnoAdminDto reservarParaPaciente(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                               @Valid @RequestBody ReservarParaPacienteRequest request) {
        return turnoService.reservarParaPaciente(id, request, principal.getUsuario());
    }

    @GetMapping("/cola-espera")
    public List<TurnoAdminDto> colaEspera(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false) Long medicoId) {
        return turnoService.colaEspera(fecha, medicoId);
    }

    @PutMapping("/{id}/asistencia")
    public TurnoAdminDto actualizarAsistencia(@PathVariable Long id, @Valid @RequestBody ActualizarAsistenciaRequest request) {
        return turnoService.actualizarAsistencia(id, request);
    }

    @GetMapping("/{id}/historial")
    public List<RegistroAuditoriaDto> historial(@PathVariable Long id) {
        return auditoriaService.buscar("TURNO", id);
    }

    @PutMapping("/{id}/deshacer")
    public TurnoAdminDto deshacer(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return turnoService.deshacerUltimaOperacion(id, principal.getUsuario());
    }

    @PutMapping("/{id}/cancelar")
    public TurnoAdminDto cancelar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id) {
        return turnoService.cancelarComoStaff(id, principal.getUsuario());
    }

    @PutMapping("/{id}/reprogramar")
    public ReprogramarTurnoResponse reprogramar(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                                 @Valid @RequestBody ReprogramarTurnoRequest request) {
        return turnoService.reprogramar(id, request.turnoDestinoId(), principal.getUsuario());
    }

    @PostMapping("/{id}/asignar-paciente")
    public TurnoAdminDto asignarPaciente(@AuthenticationPrincipal AppUserDetails principal, @PathVariable Long id,
                                          @Valid @RequestBody AsignarPacienteRequest request) {
        return turnoService.asignarTurnoAPaciente(id, request.pacienteId(), principal.getUsuario());
    }

    @PostMapping("/{id}/recordatorio/enviar")
    public RecordatorioDto enviarRecordatorioAhora(@PathVariable Long id) {
        return recordatorioService.enviarAhora(id);
    }
}
