package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    // update en bloque en vez de traer todas a memoria y guardarlas una por una — puede ser
    // una bandeja con cientos de notificaciones viejas sin leer.
    @Modifying
    @Query("update Notificacion n set n.leida = true where n.usuario.id = :usuarioId and n.leida = false")
    void marcarTodasLeidas(@Param("usuarioId") Long usuarioId);
}
