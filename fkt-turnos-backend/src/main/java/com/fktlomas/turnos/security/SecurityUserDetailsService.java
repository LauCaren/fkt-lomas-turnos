package com.fktlomas.turnos.security;

import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.util.DatosPersonales;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SecurityUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Normalizado igual que al guardar (ver Usuario.normalizarEmail). El mensaje de la
        // excepción no lleva el email: Spring la convierte en BadCredentials genérico, pero
        // podría terminar en un log.
        return usuarioRepository.findByEmail(DatosPersonales.normalizarEmail(email))
                .map(AppUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
    }
}
