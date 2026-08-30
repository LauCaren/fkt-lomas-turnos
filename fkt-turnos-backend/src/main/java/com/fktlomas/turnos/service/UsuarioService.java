package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.ActualizarPerfilRequest;
import com.fktlomas.turnos.dto.PerfilDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public PerfilDto actualizarPerfil(Usuario usuario, ActualizarPerfilRequest request) {
        if (!usuario.getEmail().equalsIgnoreCase(request.email())
                && usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictoDeDatosException("Ya existe una cuenta registrada con ese email");
        }

        usuario.setNombreCompleto(request.nombreCompleto());
        usuario.setEmail(request.email());
        usuario.setTelefono(request.telefono());
        usuario.setGenero(request.genero());
        usuario.setObraSocial(request.obraSocial());
        usuarioRepository.save(usuario);

        return PerfilDto.de(usuario);
    }
}
