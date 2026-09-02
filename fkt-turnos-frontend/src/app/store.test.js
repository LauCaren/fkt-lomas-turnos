import { describe, it, expect, beforeEach } from 'vitest';
import { store } from './store';
import { login } from '../features/auth/authSlice';

// Antes de este fix, un 401 de la API (JWT vencido/inválido) solo limpiaba
// localStorage — el store seguía con token/usuario en memoria, así que la pantalla
// protegida se quedaba renderizada como si la sesión siguiera viva mientras cada
// acción fallaba en silencio. axiosClient.js dispara este evento en cada 401;
// este test cubre que store.js efectivamente lo escucha y cierra la sesión.
describe('store — evento fkt:sesion-vencida', () => {
  beforeEach(() => {
    localStorage.clear();
    store.dispatch(login.fulfilled(
      { token: 'jwt-de-prueba', nombreCompleto: 'Test', email: 'test@test.com', role: 'PACIENTE' },
      'requestId', {}
    ));
  });

  it('un 401 de la API cierra la sesión en el store, no solo en localStorage', () => {
    expect(store.getState().auth.token).toBe('jwt-de-prueba');

    window.dispatchEvent(new Event('fkt:sesion-vencida'));

    expect(store.getState().auth.token).toBeNull();
    expect(store.getState().auth.usuario).toBeNull();
  });
});
