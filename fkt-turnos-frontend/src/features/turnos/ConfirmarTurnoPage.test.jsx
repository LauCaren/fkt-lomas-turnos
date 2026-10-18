import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import axiosClient from '../../api/axiosClient';
import ConfirmarTurnoPage from './ConfirmarTurnoPage';

// El link "No voy" del recordatorio no puede cancelar el turno solo por abrirse: los antivirus de
// correo abren los links para revisarlos. Cancela recién con un clic explícito (y por POST).
vi.mock('../../api/axiosClient', () => ({
  default: { post: vi.fn().mockResolvedValue({ data: null }) },
  mensajeDeError: (error, fallback) => fallback,
}));

function renderCon(query) {
  return render(
    <MemoryRouter initialEntries={[`/confirmar-turno?${query}`]}>
      <ConfirmarTurnoPage />
    </MemoryRouter>,
  );
}

describe('ConfirmarTurnoPage', () => {
  beforeEach(() => vi.clearAllMocks());

  it('"Sí" se confirma apenas abre la página', async () => {
    renderCon('token=abc&respuesta=si');

    expect(await screen.findByText('¡Gracias! Te esperamos')).toBeInTheDocument();
    expect(axiosClient.post).toHaveBeenCalledWith('/api/turnos/confirmacion', null, { params: { token: 'abc', respuesta: 'si' } });
  });

  it('"No" pregunta antes de cancelar y no llama a la API sin un clic', async () => {
    renderCon('token=abc&respuesta=no');

    expect(screen.getByText('¿No vas a poder venir?')).toBeInTheDocument();
    expect(axiosClient.post).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Sí, cancelar mi turno' }));

    expect(await screen.findByText('Listo, liberamos tu turno')).toBeInTheDocument();
    expect(axiosClient.post).toHaveBeenCalledWith('/api/turnos/confirmacion', null, { params: { token: 'abc', respuesta: 'no' } });
  });

  it('un link incompleto muestra el error sin llamar a la API', () => {
    renderCon('respuesta=no');

    expect(screen.getByText('El link no es válido.')).toBeInTheDocument();
    expect(axiosClient.post).not.toHaveBeenCalled();
  });
});
