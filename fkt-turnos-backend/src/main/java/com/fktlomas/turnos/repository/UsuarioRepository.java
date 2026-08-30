package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByDni(String dni);
    Optional<Usuario> findByResetToken(String resetToken);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);
}
