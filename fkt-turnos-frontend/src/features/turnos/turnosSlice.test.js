import { describe, it, expect } from 'vitest';
import turnosReducer, { fetchTurnosDisponibles } from './turnosSlice';

const ESTADO_INICIAL = turnosReducer(undefined, { type: '@@INIT' });

// Cambiar de fecha/médico rápido en BookingWizard dispara varios fetchTurnosDisponibles
// seguidos — si el de una fecha vieja resuelve después del de la fecha nueva (orden de
// red no garantizado), sin este guard pisaba la disponibilidad correcta con datos viejos.
describe('turnosSlice — fetchTurnosDisponibles descarta respuestas viejas', () => {
  it('una respuesta que llega después de un pedido más nuevo no pisa los datos actuales', () => {
    let estado = turnosReducer(ESTADO_INICIAL, fetchTurnosDisponibles.pending('request-viejo', { medicoId: 1, fecha: '2026-09-01' }));
    estado = turnosReducer(estado, fetchTurnosDisponibles.pending('request-nuevo', { medicoId: 1, fecha: '2026-09-02' }));

    const disponiblesNuevos = [{ id: 99, horaInicio: '11:00:00' }];
    estado = turnosReducer(estado, fetchTurnosDisponibles.fulfilled(disponiblesNuevos, 'request-nuevo', { medicoId: 1, fecha: '2026-09-02' }));

    // El pedido viejo resuelve tarde, después del nuevo.
    const disponiblesViejos = [{ id: 1, horaInicio: '09:00:00' }];
    estado = turnosReducer(estado, fetchTurnosDisponibles.fulfilled(disponiblesViejos, 'request-viejo', { medicoId: 1, fecha: '2026-09-01' }));

    expect(estado.disponibles).toEqual(disponiblesNuevos);
  });

  it('la respuesta más reciente sí se aplica normalmente', () => {
    let estado = turnosReducer(ESTADO_INICIAL, fetchTurnosDisponibles.pending('request-1', { medicoId: 1, fecha: '2026-09-01' }));
    const disponibles = [{ id: 1, horaInicio: '09:00:00' }];

    estado = turnosReducer(estado, fetchTurnosDisponibles.fulfilled(disponibles, 'request-1', { medicoId: 1, fecha: '2026-09-01' }));

    expect(estado.disponibles).toEqual(disponibles);
  });
});
