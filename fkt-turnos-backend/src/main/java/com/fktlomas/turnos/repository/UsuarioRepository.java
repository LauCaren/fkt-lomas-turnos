package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByDni(String dni);
    Optional<Usuario> findByResetToken(String resetToken);
    boolean existsByEmail(String email);
    boolean existsByDni(String dni);

    @Query("""
            select u from Usuario u
            where u.role = com.fktlomas.turnos.domain.Role.PACIENTE
            and (lower(u.nombreCompleto) like lower(concat('%', :busqueda, '%'))
                 or u.dni like concat('%', :busqueda, '%'))
            order by u.nombreCompleto asc
            """)
    List<Usuario> buscarPacientes(@Param("busqueda") String busqueda);
}
