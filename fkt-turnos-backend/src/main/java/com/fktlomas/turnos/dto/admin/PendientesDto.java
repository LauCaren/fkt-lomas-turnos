package com.fktlomas.turnos.dto.admin;

/**
 * Los contadores de "cosas para atender" que alimentan los numeritos del menú lateral de la
 * secretaría. Es un subconjunto de {@link AdminResumenDto} sin ningún dato de caja (ingresos,
 * caja física), que la secretaría no tiene que ver — por eso un endpoint propio en vez de abrirle
 * el resumen completo del admin.
 */
public record PendientesDto(
        int turnosSinConfirmarHoy,
        int pacientesEnEspera,
        int mensajesSinLeer,
        int postulacionesSinRevisar,
        int solicitudesBajaPendientes,
        int arrepentimientosPendientes
) {
}
