package com.fktlomas.turnos.service;

import com.fktlomas.turnos.domain.*;
import com.fktlomas.turnos.dto.caja.CajaResumenDto;
import com.fktlomas.turnos.dto.caja.CrearIngresoRequest;
import com.fktlomas.turnos.dto.caja.CrearRetiroRequest;
import com.fktlomas.turnos.dto.caja.IngresoDto;
import com.fktlomas.turnos.dto.caja.SaldoMedicoDto;
import com.fktlomas.turnos.exception.ConflictoDeDatosException;
import com.fktlomas.turnos.repository.EspecialidadRepository;
import com.fktlomas.turnos.repository.MedicoRepository;
import com.fktlomas.turnos.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cubre la parte de caja con más riesgo de un bug silencioso: los cálculos de saldo. Un
 * ingreso anulado no debe contar en ningún total, "caja física" tiene que descontar los
 * gastos (a diferencia del saldo por médico, que no los toca — son dos números distintos
 * a propósito, ver CajaService), y no se puede retirar más de lo que un médico acumuló.
 *
 * {@code @Transactional} acá es necesario, no cosmético: sin esto, cada método de test
 * comparte la misma base H2 en memoria (el contexto de Spring se cachea entre tests de la
 * misma clase) y los totales GLOBALES de resumen() (no filtrados por médico) se contaminan
 * entre tests según el orden en que corran. Con @Transactional cada test hace rollback al
 * terminar.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:caja-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class CajaServiceTest {

    @Autowired
    private CajaService cajaService;
    @Autowired
    private GastoService gastoService;
    @Autowired
    private EspecialidadRepository especialidadRepository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void elResumenExcluyeIngresosAnuladosYCajaFisicaDescuentaGastosPeroElSaldoPorMedicoNo() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medicoA = crearMedico("Dra. A");
        Medico medicoB = crearMedico("Dr. B");

        IngresoDto ingreso1000 = cajaService.registrarIngreso(
                new CrearIngresoRequest(medicoA.getId(), null, new BigDecimal("1000"), MetodoPago.EFECTIVO, null), secretaria);
        IngresoDto ingreso500 = cajaService.registrarIngreso(
                new CrearIngresoRequest(medicoA.getId(), null, new BigDecimal("500"), MetodoPago.TRANSFERENCIA, null), secretaria);
        cajaService.registrarIngreso(
                new CrearIngresoRequest(medicoB.getId(), null, new BigDecimal("200"), MetodoPago.MERCADOPAGO, null), secretaria);
        cajaService.registrarRetiro(new CrearRetiroRequest(medicoA.getId(), new BigDecimal("300")), secretaria);

        cajaService.anularIngreso(ingreso500.id(), "Cobrado por error", secretaria);

        gastoService.crearCategoria(new com.fktlomas.turnos.dto.caja.CrearCategoriaGastoRequest("Insumos test"));
        Long categoriaId = gastoService.listarCategorias().stream()
                .filter(c -> c.nombre().equals("Insumos test")).findFirst().orElseThrow().id();
        gastoService.registrar(categoriaId, new BigDecimal("100"), null, "Guantes", null, secretaria);

        CajaResumenDto resumen = cajaService.resumen();

        assertThat(resumen.ingresosTotal()).isEqualByComparingTo("1200");
        assertThat(resumen.retirosTotal()).isEqualByComparingTo("300");
        assertThat(resumen.gastosTotal()).isEqualByComparingTo("100");
        assertThat(resumen.cajaFisica()).isEqualByComparingTo("800");

        SaldoMedicoDto saldoA = resumen.saldosPorMedico().stream().filter(s -> s.medicoId().equals(medicoA.getId())).findFirst().orElseThrow();
        assertThat(saldoA.ingresosTotal()).isEqualByComparingTo("1000");
        assertThat(saldoA.retirosTotal()).isEqualByComparingTo("300");
        assertThat(saldoA.saldo()).isEqualByComparingTo("700");

        SaldoMedicoDto saldoDirecto = cajaService.saldoDeMedico(medicoA.getId());
        assertThat(saldoDirecto.saldo()).isEqualByComparingTo("700");

        assertThat(ingreso1000.anulado()).isFalse();
        assertThat(cajaService.listarIngresos(medicoA.getId(), null, null))
                .filteredOn(i -> i.id().equals(ingreso500.id())).first()
                .satisfies(i -> assertThat(i.anulado()).isTrue());
    }

    @Test
    void noSePuedeRetirarMasDeLoQueElMedicoTieneAcumulado() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medico = crearMedico("Dr. Sin Saldo");
        cajaService.registrarIngreso(
                new CrearIngresoRequest(medico.getId(), null, new BigDecimal("200"), MetodoPago.EFECTIVO, null), secretaria);

        assertThatThrownBy(() -> cajaService.registrarRetiro(new CrearRetiroRequest(medico.getId(), new BigDecimal("500")), secretaria))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void noSePuedeAnularUnIngresoDosVeces() {
        Usuario secretaria = crearUsuarioAdmin();
        Medico medico = crearMedico("Dr. Anulado");
        IngresoDto ingreso = cajaService.registrarIngreso(
                new CrearIngresoRequest(medico.getId(), null, new BigDecimal("100"), MetodoPago.EFECTIVO, null), secretaria);

        cajaService.anularIngreso(ingreso.id(), "Motivo 1", secretaria);

        assertThatThrownBy(() -> cajaService.anularIngreso(ingreso.id(), "Motivo 2", secretaria))
                .isInstanceOf(ConflictoDeDatosException.class);
    }

    private Medico crearMedico(String nombre) {
        Especialidad especialidad = new Especialidad();
        especialidad.setNombre(nombre + " especialidad " + System.nanoTime());
        especialidad = especialidadRepository.save(especialidad);

        Medico medico = new Medico();
        medico.setNombreCompleto(nombre);
        medico.setEspecialidad(especialidad);
        medico.setDuracionTurnoMinutos(30);
        return medicoRepository.save(medico);
    }

    private Usuario crearUsuarioAdmin() {
        Usuario usuario = new Usuario();
        usuario.setEmail("secretaria-" + System.nanoTime() + "@test.com");
        usuario.setNombreCompleto("Secretaria Test");
        usuario.setTelefono("1122334455");
        usuario.setObraSocial("Particular");
        usuario.setPasswordHash("hash");
        usuario.setRole(Role.ADMIN);
        return usuarioRepository.save(usuario);
    }
}
