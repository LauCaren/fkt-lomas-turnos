import { describe, it, expect } from 'vitest';
import cajaReducer, { registrarRetiro, anularRetiro } from './cajaSlice';

const ESTADO_INICIAL = cajaReducer(undefined, { type: '@@INIT' });

describe('cajaSlice — retiro de caja', () => {
  it('registrarRetiro.fulfilled lo agrega arriba de la lista y avisa el éxito', () => {
    const retiroExistente = { id: 1, monto: 500 };
    const retiroNuevo = { id: 2, monto: 1000 };
    const estadoConUnRetiro = { ...ESTADO_INICIAL, retiros: [retiroExistente] };

    const estado = cajaReducer(estadoConUnRetiro, registrarRetiro.fulfilled(retiroNuevo, 'requestId', {}));

    expect(estado.retiros).toEqual([retiroNuevo, retiroExistente]);
    expect(estado.mensajeExito).toBe('Retiro registrado');
  });

  it('registrarRetiro.rejected (ej. supera el saldo del médico) guarda el error sin agregar nada a la lista', () => {
    const estado = cajaReducer(ESTADO_INICIAL, registrarRetiro.rejected(new Error('fallo'), 'requestId', {}, 'El monto supera el saldo disponible del médico'));

    expect(estado.error).toMatch(/supera el saldo/i);
    expect(estado.retiros).toEqual([]);
  });

  it('anularRetiro.fulfilled reemplaza ese retiro por su versión anulada, sin tocar los demás', () => {
    const estadoConDosRetiros = { ...ESTADO_INICIAL, retiros: [{ id: 1, anulado: false }, { id: 2, anulado: false }] };

    const estado = cajaReducer(estadoConDosRetiros, anularRetiro.fulfilled({ id: 1, anulado: true, motivoAnulacion: 'error de carga' }, 'requestId', { id: 1, motivo: 'error de carga' }));

    expect(estado.retiros).toEqual([{ id: 1, anulado: true, motivoAnulacion: 'error de carga' }, { id: 2, anulado: false }]);
  });
});
