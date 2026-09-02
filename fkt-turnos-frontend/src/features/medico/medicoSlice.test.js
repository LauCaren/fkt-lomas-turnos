import { describe, it, expect } from 'vitest';
import medicoReducer, { crearEvolucion, marcarAtendido, fetchEvolucionesDePaciente } from './medicoSlice';

const ESTADO_INICIAL = medicoReducer(undefined, { type: '@@INIT' });

describe('medicoSlice', () => {
  it('crearEvolucion.fulfilled avisa que se guardó la evolución clínica', () => {
    const estado = medicoReducer(ESTADO_INICIAL, crearEvolucion.fulfilled({ id: 1, texto: 'Mejoró el rango de movimiento' }, 'requestId', { turnoId: 5, texto: 'x' }));

    expect(estado.mensajeExito).toBe('Evolución guardada');
  });

  it('crearEvolucion.rejected guarda el error sin marcar éxito', () => {
    const estado = medicoReducer(ESTADO_INICIAL, crearEvolucion.rejected(new Error('fallo'), 'requestId', { turnoId: 5, texto: 'x' }, 'No se pudo guardar la evolución'));

    expect(estado.error).toBe('No se pudo guardar la evolución');
    expect(estado.mensajeExito).toBeNull();
  });

  it('marcarAtendido.fulfilled saca al paciente de la cola de espera', () => {
    const estadoConCola = { ...ESTADO_INICIAL, colaEspera: [{ id: 10 }, { id: 20 }] };

    const estado = medicoReducer(estadoConCola, marcarAtendido.fulfilled(10, 'requestId', 10));

    expect(estado.colaEspera).toEqual([{ id: 20 }]);
    expect(estado.mensajeExito).toBe('Paciente marcado como atendido');
  });

  it('fetchEvolucionesDePaciente guarda el historial bajo el id del paciente correspondiente, sin pisar el de otros pacientes', () => {
    const estadoConHistorialDeOtroPaciente = { ...ESTADO_INICIAL, evolucionesPorPaciente: { 7: [{ id: 1, texto: 'previa' }] } };

    const estado = medicoReducer(
      estadoConHistorialDeOtroPaciente,
      fetchEvolucionesDePaciente.fulfilled({ pacienteId: 9, evoluciones: [{ id: 2, texto: 'nueva' }] }, 'requestId', 9)
    );

    expect(estado.evolucionesPorPaciente[9]).toEqual([{ id: 2, texto: 'nueva' }]);
    expect(estado.evolucionesPorPaciente[7]).toEqual([{ id: 1, texto: 'previa' }]);
  });
});
