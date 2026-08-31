package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.repository.UsuarioRepository;
import com.fktlomas.turnos.validation.Patrones;
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

    /**
     * Coincide con el default de app.admin.bootstrap-password en application.yml — un valor
     * público (está en el código fuente), así que nunca se usa para crear la cuenta admin.
     */
    private static final String PASSWORD_POR_DEFECTO = "cambiar-esta-clave";

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
        if (PASSWORD_POR_DEFECTO.equals(bootstrapPassword)) {
            log.error("No se creó el usuario admin ({}): falta configurar ADMIN_BOOTSTRAP_PASSWORD con una "
                    + "contraseña propia — el valor por defecto es público (está en el código fuente) y nunca se usa "
                    + "para crear la cuenta. Configurá la variable de entorno y reiniciá la aplicación.", bootstrapEmail);
            return;
        }
        if (!bootstrapPassword.matches(Patrones.PASSWORD_FUERTE)) {
            log.error("No se creó el usuario admin ({}): ADMIN_BOOTSTRAP_PASSWORD no cumple los requisitos de "
                    + "seguridad ({}).", bootstrapEmail, Patrones.PASSWORD_FUERTE_MENSAJE);
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
