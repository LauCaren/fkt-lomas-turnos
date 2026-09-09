package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.CanalNotificacion;
import com.fktlomas.turnos.domain.Role;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Crea (si no existe) el usuario de sistema que actúa como actor de auditoría cuando
 * WhatsAppBotService reserva/cancela un turno en nombre de un paciente identificado por DNI —
 * mismo rol de confianza que hoy tiene una secretaria en reservarParaPaciente, solo que
 * automatizado. Mismo patrón que AdminBootstrapRunner: idempotente, contraseña aleatoria que
 * nadie necesita conocer (nadie tiene que loguearse como este usuario).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppBotAccountInitializer implements CommandLineRunner {

    public static final String EMAIL_BOT = "bot-whatsapp" + Usuario.DOMINIO_EMAIL_SINTETICO;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmail(EMAIL_BOT)) {
            return;
        }
        Usuario bot = new Usuario();
        bot.setEmail(EMAIL_BOT);
        bot.setNombreCompleto("Bot WhatsApp FKT Lomas");
        bot.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        bot.setRole(Role.SECRETARIA);
        bot.setAceptaPoliticaPrivacidad(true);
        bot.setCanalNotificacionPreferido(CanalNotificacion.EMAIL);
        // Inhabilitado para login (ver AppUserDetails.isEnabled()) — segunda barrera explícita
        // y auditable además de que nadie conoce la contraseña aleatoria.
        bot.setActivo(false);
        usuarioRepository.save(bot);
        log.info("Usuario de sistema del bot de WhatsApp creado ({})", EMAIL_BOT);
    }
}
