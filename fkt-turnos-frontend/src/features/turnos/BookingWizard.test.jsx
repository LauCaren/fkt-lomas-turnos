import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import turnosReducer from './turnosSlice';
import cuentaReducer from '../cuenta/cuentaSlice';
import BookingWizard from './BookingWizard';

// El aviso de copago IOMA es la regla de negocio más delicada del wizard de
// reserva: si no se muestra, un paciente de IOMA puede terminar en el
// consultorio sin saber que tiene que pagar un adicional. Se decide con dos
// datos que llegan de lugares distintos — perfil.obraSocial (cuentaSlice) y
// medico.cobraCopagoIoma (turnosSlice) — así que vale la pena un test
// específico de esa combinación, no solo probar el flujo "feliz".
vi.mock('../../api/axiosClient', () => ({
  default: { get: vi.fn().mockResolvedValue({ data: [] }), post: vi.fn() },
  mensajeDeError: (error, fallback) => error?.response?.data?.mensaje || fallback,
}));

const MEDICO_CON_COPAGO = { id: 1, nombreCompleto: 'Dra. Test', cobraCopagoIoma: true };
const MEDICO_SIN_COPAGO = { id: 2, nombreCompleto: 'Dr. Sin Copago', cobraCopagoIoma: false };

const RESERVA_EXITOSA = {
  medicoId: 1,
  medicoNombre: 'Dra. Test',
  especialidadNombre: 'Traumatología',
  fecha: '2026-09-10',
  horaInicio: '10:00:00',
  horaFin: '10:30:00',
};

function renderConEstado({ obraSocial, medico }) {
  const store = configureStore({
    reducer: { turnos: turnosReducer, cuenta: cuentaReducer },
    preloadedState: {
      turnos: {
        especialidades: [], medicos: [medico], disponibles: [], misTurnos: [],
        status: 'idle', error: null, reservaExitosa: { ...RESERVA_EXITOSA, medicoId: medico.id, medicoNombre: medico.nombreCompleto },
        miAnotacionListaEspera: null,
      },
      cuenta: { perfil: { obraSocial }, mfaSetup: null, error: null, mensajeExito: null },
    },
  });
  render(
    <Provider store={store}>
      <MemoryRouter>
        <BookingWizard />
      </MemoryRouter>
    </Provider>
  );
}

describe('BookingWizard — aviso de copago IOMA', () => {
  beforeEach(() => vi.clearAllMocks());

  it('avisa por WhatsApp si el paciente es de IOMA y el médico cobra copago', () => {
    renderConEstado({ obraSocial: 'IOMA', medico: MEDICO_CON_COPAGO });

    expect(screen.getByText(/cobra un copago a pacientes con IOMA/i)).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /escribinos por whatsapp/i })).toHaveAttribute(
      'href',
      expect.stringContaining('wa.me')
    );
  });

  it('no avisa si el paciente es de IOMA pero el médico no cobra copago', () => {
    renderConEstado({ obraSocial: 'IOMA', medico: MEDICO_SIN_COPAGO });

    expect(screen.queryByText(/cobra un copago/i)).not.toBeInTheDocument();
  });

  it('no avisa si el médico cobra copago pero el paciente no es de IOMA', () => {
    renderConEstado({ obraSocial: 'Particular', medico: MEDICO_CON_COPAGO });

    expect(screen.queryByText(/cobra un copago/i)).not.toBeInTheDocument();
  });

  it('siempre muestra la confirmación del turno reservado, con o sin copago', () => {
    renderConEstado({ obraSocial: 'Particular', medico: MEDICO_SIN_COPAGO });

    expect(screen.getByText('¡Turno reservado con éxito!')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: /Traumatología con Dr\. Sin Copago/ })).toBeInTheDocument();
  });
});
