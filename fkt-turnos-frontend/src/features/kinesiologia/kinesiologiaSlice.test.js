import { describe, it, expect } from 'vitest';
import kinesiologiaReducer, {
  crearPlanKinesiologia, fetchHorariosDisponiblesKinesiologia, limpiarHorariosDisponibles,
} from './kinesiologiaSlice';

const ESTADO_INICIAL = kinesiologiaReducer(undefined, { type: '@@INIT' });

describe('kinesiologiaSlice', () => {
  it('crearPlan.fulfilled agrega el plan a "mis planes" y avisa que queda pendiente de revisión', () => {
    const plan = { id: 1, estado: 'PENDIENTE_VALIDACION', sesiones: [] };

    const estado = kinesiologiaReducer(ESTADO_INICIAL, crearPlanKinesiologia.fulfilled(plan, 'requestId', new FormData()));

    expect(estado.misPlanes).toEqual([plan]);
    expect(estado.mensajeExito).toMatch(/admin va a revisar la orden médica/i);
  });

  it('crearPlan.rejected (p. ej. horario fuera de la regla del médico) guarda el error sin tocar los planes existentes', () => {
    const estadoConUnPlan = { ...ESTADO_INICIAL, misPlanes: [{ id: 1, estado: 'VALIDADO' }] };
    const rejectedAction = crearPlanKinesiologia.rejected(
      new Error('fallo'), 'requestId', new FormData(), 'El médico no atiende los lunes a las 15:00 según su horario cargado'
    );

    const estado = kinesiologiaReducer(estadoConUnPlan, rejectedAction);

    expect(estado.error).toMatch(/no atiende los lunes/i);
    expect(estado.misPlanes).toHaveLength(1);
  });

  it('fetchHorariosDisponibles guarda los horarios reales del médico elegido (o de todos, si no se pasa medicoId)', () => {
    const horarios = [
      { medicoId: 1, medicoNombre: 'Lic. Test', diaSemana: 'MONDAY', horaInicio: '09:00:00' },
      { medicoId: 2, medicoNombre: 'Lic. Otro', diaSemana: 'TUESDAY', horaInicio: '10:00:00' },
    ];

    let estado = kinesiologiaReducer(ESTADO_INICIAL, fetchHorariosDisponiblesKinesiologia.pending('requestId', undefined));
    expect(estado.cargandoHorarios).toBe(true);

    estado = kinesiologiaReducer(estado, fetchHorariosDisponiblesKinesiologia.fulfilled(horarios, 'requestId', undefined));
    expect(estado.cargandoHorarios).toBe(false);
    expect(estado.horariosDisponibles).toEqual(horarios);
  });

  it('limpiarHorariosDisponibles vacía la lista al cambiar de médico', () => {
    const estadoConHorarios = { ...ESTADO_INICIAL, horariosDisponibles: [{ medicoId: 1 }] };

    const estado = kinesiologiaReducer(estadoConHorarios, limpiarHorariosDisponibles());

    expect(estado.horariosDisponibles).toEqual([]);
  });
});
