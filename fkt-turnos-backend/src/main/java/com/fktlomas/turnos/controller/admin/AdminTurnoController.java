package com.fktlomas.turnos.controller.admin;

import com.fktlomas.turnos.domain.EstadoTurno;
import com.fktlomas.turnos.dto.TurnoAdminDto;
import com.fktlomas.turnos.dto.admin.ReservarParaPacienteRequest;
import com.fktlomas.turnos.service.TurnoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/turnos")
@RequiredArgsConstructor
public class AdminTurnoController {

    private final TurnoService turnoService;

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
    public TurnoAdminDto reservarParaPaciente(@PathVariable Long id, @Valid @RequestBody ReservarParaPacienteRequest request) {
        return turnoService.reservarParaPaciente(id, request);
    }
}
