package com.fktlomas.turnos.repository;

import com.fktlomas.turnos.domain.EstadoPagoOnline;
import com.fktlomas.turnos.domain.PagoOnline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PagoOnlineRepository extends JpaRepository<PagoOnline, Long> {

    List<PagoOnline> findByTurnoIdOrderByCreatedAtDesc(Long turnoId);

    /** Un turno solo puede tener un pago en curso o confirmado a la vez — ver MercadoPagoService.crearPreferencia. */
    boolean existsByTurnoIdAndEstadoIn(Long turnoId, List<EstadoPagoOnline> estados);

    Optional<PagoOnline> findByPreferenceId(String preferenceId);

    Optional<PagoOnline> findByPaymentId(String paymentId);
}
