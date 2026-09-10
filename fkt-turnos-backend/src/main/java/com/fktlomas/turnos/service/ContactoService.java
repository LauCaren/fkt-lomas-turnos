package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.MensajeContacto;
import com.fktlomas.turnos.dto.ContactoRequest;
import com.fktlomas.turnos.dto.MensajeContactoDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.MensajeContactoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContactoService {

    private final MensajeContactoRepository mensajeContactoRepository;
    private final EmailService emailService;

    public void recibirMensaje(ContactoRequest request) {
        MensajeContacto mensaje = new MensajeContacto();
        mensaje.setNombre(request.nombre());
        mensaje.setEmail(request.email());
        mensaje.setTelefono(request.telefono());
        mensaje.setMensaje(request.mensaje());
        mensajeContactoRepository.save(mensaje);
        emailService.notificarNuevoMensajeContacto(mensaje);
    }

    // Paginado (no findAll() completo): esta bandeja crece indefinidamente con los años de
    // uso del centro, a diferencia de tablas chicas como especialidades/médicos.
    public Page<MensajeContactoDto> listar(Pageable pageable) {
        Pageable ordenado = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending());
        return mensajeContactoRepository.findAll(ordenado).map(MensajeContactoDto::de);
    }

    public MensajeContactoDto marcarLeido(Long id) {
        MensajeContacto mensaje = mensajeContactoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado"));
        mensaje.setLeido(true);
        return MensajeContactoDto.de(mensajeContactoRepository.save(mensaje));
    }
}
