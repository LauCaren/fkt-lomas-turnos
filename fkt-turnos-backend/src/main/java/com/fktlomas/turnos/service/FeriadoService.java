package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Feriado;
import com.fktlomas.turnos.domain.TipoFeriado;
import com.fktlomas.turnos.dto.FeriadoDto;
import com.fktlomas.turnos.dto.admin.FeriadoRequest;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.FeriadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeriadoService {

    private final FeriadoRepository feriadoRepository;

    @Transactional(readOnly = true)
    public List<FeriadoDto> listar(LocalDate desde, LocalDate hasta) {
        return feriadoRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta).stream().map(FeriadoDto::de).toList();
    }

    /** Fechas feriadas en el rango, para que otros servicios (ej. generación de turnos) las salteen sin N+1 queries. */
    @Transactional(readOnly = true)
    public Set<LocalDate> fechasEnRango(LocalDate desde, LocalDate hasta) {
        return feriadoRepository.findByFechaBetweenOrderByFechaAsc(desde, hasta).stream()
                .map(Feriado::getFecha)
                .collect(Collectors.toSet());
    }

    @Transactional
    public FeriadoDto crear(FeriadoRequest request) {
        if (feriadoRepository.existsByFecha(request.fecha())) {
            throw new ConflictoDeDatosException("Ya hay un feriado cargado para esa fecha");
        }
        Feriado feriado = new Feriado();
        feriado.setFecha(request.fecha());
        feriado.setNombre(request.nombre());
        feriado.setTipo(TipoFeriado.CENTRO);
        return FeriadoDto.de(feriadoRepository.save(feriado));
    }

    @Transactional
    public void eliminar(Long id) {
        if (!feriadoRepository.existsById(id)) {
            throw new RecursoNoEncontradoException("Feriado no encontrado");
        }
        feriadoRepository.deleteById(id);
    }
}
