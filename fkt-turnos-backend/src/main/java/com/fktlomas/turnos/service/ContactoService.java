package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.MensajeContacto;
import com.fktlomas.turnos.dto.ContactoRequest;
import com.fktlomas.turnos.dto.MensajeContactoDto;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.MensajeContactoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<MensajeContactoDto> listar() {
        return mensajeContactoRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(MensajeContactoDto::de).toList();
    }

    public MensajeContactoDto marcarLeido(Long id) {
        MensajeContacto mensaje = mensajeContactoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mensaje no encontrado"));
        mensaje.setLeido(true);
        return MensajeContactoDto.de(mensajeContactoRepository.save(mensaje));
    }
}
