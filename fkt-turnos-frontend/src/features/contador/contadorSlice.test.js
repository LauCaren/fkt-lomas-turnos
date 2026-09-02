import { describe, it, expect } from 'vitest';
import contadorReducer, {
  fetchResumenCajaContador, fetchRetirosContador, fetchIngresosContador, fetchGastosContador,
} from './contadorSlice';

const ESTADO_INICIAL = contadorReducer(undefined, { type: '@@INIT' });

describe('contadorSlice', () => {
  it('fetchResumen guarda el resumen de caja, con el saldo por médico incluido', () => {
    const resumen = { ingresosTotal: 5000, retirosTotal: 0, gastosTotal: 0, cajaFisica: 5000, saldosPorMedico: [{ medicoId: 1, medicoNombre: 'Dr. Test', saldo: 5000 }] };

    const estado = contadorReducer(ESTADO_INICIAL, fetchResumenCajaContador.fulfilled(resumen, 'requestId', undefined));

    expect(estado.resumen).toEqual(resumen);
    expect(estado.resumenCargando).toBe(false);
  });

  it('fetchRetiros guarda la lista de retiros — el CONTADOR es de solo lectura, no hay thunk para registrar uno', () => {
    const retiros = [{ id: 1, medicoNombre: 'Dr. Test', monto: 1000 }];

    let estado = contadorReducer(ESTADO_INICIAL, fetchRetirosContador.pending('requestId', undefined));
    expect(estado.retirosCargando).toBe(true);

    estado = contadorReducer(estado, fetchRetirosContador.fulfilled(retiros, 'requestId', undefined));
    expect(estado.retiros).toEqual(retiros);
    expect(estado.retirosCargando).toBe(false);
  });

  it('un fetch rechazado (ej. 403 por rol) guarda el mensaje de error sin tocar los datos ya cargados', () => {
    const estadoConIngresos = { ...ESTADO_INICIAL, ingresos: [{ id: 1 }] };

    const estado = contadorReducer(estadoConIngresos, fetchIngresosContador.rejected(new Error('fallo'), 'requestId', undefined, 'No autorizado'));

    expect(estado.error).toBe('No autorizado');
    expect(estado.ingresos).toEqual([{ id: 1 }]);
  });

  it('fetchGastos guarda la lista de gastos', () => {
    const gastos = [{ id: 1, categoriaNombre: 'Alquiler', monto: 2000 }];

    const estado = contadorReducer(ESTADO_INICIAL, fetchGastosContador.fulfilled(gastos, 'requestId', undefined));

    expect(estado.gastos).toEqual(gastos);
  });
});
