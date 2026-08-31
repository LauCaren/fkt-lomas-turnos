import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import authReducer from './authSlice';
import LoginPage from './LoginPage';
import axiosClient from '../../api/axiosClient';

vi.mock('../../api/axiosClient', () => ({
  default: { post: vi.fn() },
  mensajeDeError: (error, fallback) => error?.response?.data?.mensaje || fallback,
}));

function renderConStore() {
  const store = configureStore({ reducer: { auth: authReducer } });
  render(
    <Provider store={store}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </Provider>
  );
  return store;
}

describe('LoginPage', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('muestra los campos de email y contraseña', () => {
    renderConStore();

    expect(screen.getByLabelText('Email')).toBeInTheDocument();
    expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ingresar' })).toBeInTheDocument();
  });

  it('ante credenciales inválidas muestra el mensaje de error devuelto por el backend', async () => {
    axiosClient.post.mockRejectedValueOnce({ response: { data: { mensaje: 'Credenciales inválidas' } } });
    const usuario = userEvent.setup();
    renderConStore();

    await usuario.type(screen.getByLabelText('Email'), 'paciente@test.com');
    await usuario.type(screen.getByLabelText('Contraseña'), 'ClaveIncorrecta1!');
    await usuario.click(screen.getByRole('button', { name: 'Ingresar' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Credenciales inválidas');
  });

  it('ante un login exitoso guarda la sesión en el store', async () => {
    axiosClient.post.mockResolvedValueOnce({
      data: { token: 'jwt-123', nombreCompleto: 'Paciente Test', email: 'paciente@test.com', role: 'PACIENTE' },
    });
    const usuario = userEvent.setup();
    const store = renderConStore();

    await usuario.type(screen.getByLabelText('Email'), 'paciente@test.com');
    await usuario.type(screen.getByLabelText('Contraseña'), 'Segura123!');
    await usuario.click(screen.getByRole('button', { name: 'Ingresar' }));

    await waitFor(() => expect(store.getState().auth.token).toBe('jwt-123'));
  });
});
