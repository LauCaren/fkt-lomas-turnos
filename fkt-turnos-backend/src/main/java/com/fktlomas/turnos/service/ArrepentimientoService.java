package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.EstadoArrepentimiento;
import com.fktlomas.turnos.domain.SolicitudArrepentimiento;
import com.fktlomas.turnos.domain.Usuario;
import com.fktlomas.turnos.dto.arrepentimiento.ArrepentimientoRecibidoDto;
import com.fktlomas.turnos.dto.arrepentimiento.ArrepentimientoRequest;
import com.fktlomas.turnos.dto.arrepentimiento.SolicitudArrepentimientoDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.exception.DemasiadosIntentosException;
import com.fktlomas.turnos.exception.RecursoNoEncontradoException;
import com.fktlomas.turnos.repository.SolicitudArrepentimientoRepository;
import com.fktlomas.turnos.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * "Botón de arrepentimiento" (Ley 24.240 art. 34, Resolución SCI 424/2020). Flujo:
 * <ol>
 *   <li>La persona completa el formulario público (sitio o app, sin iniciar sesión).</li>
 *   <li>Se genera un código de trámite al instante: se le muestra en pantalla y se le manda por
 *       mail (la norma pide entregarlo dentro de las 24 h por el mismo medio).</li>
 *   <li>Le llega un aviso al admin, y el pedido queda en la bandeja "Arrepentimiento" del panel.</li>
 *   <li>El centro lo gestiona a mano (cancela el turno, devuelve el pago por Mercado Pago) y lo
 *       marca como resuelto con una nota que queda como constancia.</li>
 * </ol>
 * La devolución del dinero no es automática a propósito: depende de cómo se pagó (Mercado Pago,
 * efectivo, obra social) y conviene que la revise una persona.
 */
@Service
@RequiredArgsConstructor
public class ArrepentimientoService {

    // Sin 0/O ni 1/I/L, para que el código se pueda dictar por teléfono sin confusiones.
    private static final String ALFABETO_CODIGO = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final int LARGO_CODIGO = 8;
    private static final int MAX_PEDIDOS_POR_EMAIL_POR_DIA = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SolicitudArrepentimientoRepository repository;
    private final EmailService emailService;
    private final RateLimiterService rateLimiterService;

    @Transactional
    public ArrepentimientoRecibidoDto recibir(ArrepentimientoRequest request) {
        if (request.pareceSpam()) {
            throw new IllegalArgumentException("No pudimos procesar el pedido");
        }
        if (!rateLimiterService.permitir("arrepentimiento|" + request.email(), MAX_PEDIDOS_POR_EMAIL_POR_DIA, Duration.ofDays(1))) {
            throw new DemasiadosIntentosException("Ya recibimos varios pedidos desde este email hoy. Si necesitás algo más, escribinos por WhatsApp.");
        }

        SolicitudArrepentimiento solicitud = new SolicitudArrepentimiento();
        solicitud.setCodigo(codigoNuevo());
        solicitud.setNombreCompleto(request.nombreCompleto().trim());
        solicitud.setDni(vacioANull(request.dni()));
        solicitud.setEmail(request.email());
        solicitud.setTelefono(vacioANull(request.telefono()));
        solicitud.setDetalleOperacion(request.detalleOperacion().trim());
        repository.save(solicitud);

        emailService.enviarCodigoArrepentimiento(solicitud);
        emailService.notificarNuevoArrepentimiento(solicitud);
        return new ArrepentimientoRecibidoDto(solicitud.getCodigo());
    }

    @Transactional(readOnly = true)
    public List<SolicitudArrepentimientoDto> listar(EstadoArrepentimiento estado) {
        return repository.buscar(estado).stream().map(SolicitudArrepentimientoDto::de).toList();
    }

    @Transactional
    public SolicitudArrepentimientoDto resolver(Long id, Usuario actor, String nota) {
        SolicitudArrepentimiento solicitud = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado"));
        if (solicitud.getEstado() == EstadoArrepentimiento.RESUELTA) {
            throw new ConflictoDeDatosException("Este pedido ya estaba resuelto");
        }
        solicitud.setEstado(EstadoArrepentimiento.RESUELTA);
        solicitud.setNotaResolucion(nota.trim());
        solicitud.setResueltaPor(actor);
        solicitud.setResueltaAt(Instant.now());
        return SolicitudArrepentimientoDto.de(repository.save(solicitud));
    }

    private String codigoNuevo() {
        // 31^8 ≈ 850 mil millones de combinaciones: una colisión es casi imposible, pero se
        // chequea igual porque la columna es UNIQUE y un choque rompería el pedido.
        String codigo;
        do {
            StringBuilder sb = new StringBuilder("ARR-");
            for (int i = 0; i < LARGO_CODIGO; i++) {
                sb.append(ALFABETO_CODIGO.charAt(RANDOM.nextInt(ALFABETO_CODIGO.length())));
            }
            codigo = sb.toString();
        } while (repository.existsByCodigo(codigo));
        return codigo;
    }

    private static String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
