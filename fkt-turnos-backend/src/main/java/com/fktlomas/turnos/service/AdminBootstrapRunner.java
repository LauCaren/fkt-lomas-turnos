package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.bootstrap-email}")
    private String bootstrapEmail;

    @Value("${app.admin.bootstrap-password}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmail(bootstrapEmail)) {
            return;
        }
        Usuario admin = new Usuario();
        admin.setEmail(bootstrapEmail);
        admin.setNombreCompleto("Fernando Carenzio");
        admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
        admin.setRole(Role.ADMIN);
        usuarioRepository.save(admin);
        log.info("Usuario admin creado ({}). Cambiá la contraseña bootstrap después del primer login.", bootstrapEmail);
    }
}
