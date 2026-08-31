import { describe, it, expect, beforeEach } from 'vitest';
import authReducer, { login, logout } from './authSlice';

describe('authSlice', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('login.fulfilled guarda el usuario, el token y persiste la sesión en localStorage', () => {
    const estadoInicial = authReducer(undefined, { type: '@@INIT' });
    const payload = { token: 'jwt-de-prueba', nombreCompleto: 'Fernando', email: 'fernando@fktlomas.com', role: 'ADMIN' };

    const estado = authReducer(estadoInicial, login.fulfilled(payload, 'requestId', {}));

    expect(estado.token).toBe('jwt-de-prueba');
    expect(estado.usuario).toEqual({ nombreCompleto: 'Fernando', email: 'fernando@fktlomas.com', role: 'ADMIN' });
    expect(estado.requiereMfa).toBe(false);
    expect(localStorage.getItem('fkt_token')).toBe('jwt-de-prueba');
    expect(JSON.parse(localStorage.getItem('fkt_user'))).toEqual(estado.usuario);
  });

  it('login.rejected guarda el mensaje de error y la bandera de MFA sin tocar la sesión existente', () => {
    const estadoInicial = { ...authReducer(undefined, { type: '@@INIT' }), token: 'token-viejo', usuario: { email: 'x@x.com' } };
    const rejectedAction = login.rejected(new Error('fallo'), 'requestId', {}, { mensaje: 'Falta el código de verificación', requiereMfa: true });

    const estado = authReducer(estadoInicial, rejectedAction);

    expect(estado.error).toBe('Falta el código de verificación');
    expect(estado.requiereMfa).toBe(true);
    expect(estado.token).toBe('token-viejo');
  });

  it('logout limpia el estado y localStorage', () => {
    localStorage.setItem('fkt_token', 'algo');
    localStorage.setItem('fkt_user', JSON.stringify({ email: 'x@x.com' }));
    const estadoConSesion = { token: 'algo', usuario: { email: 'x@x.com' }, status: 'idle', error: null, requiereMfa: false, mensajeExito: null };

    const estado = authReducer(estadoConSesion, logout());

    expect(estado.token).toBeNull();
    expect(estado.usuario).toBeNull();
    expect(localStorage.getItem('fkt_token')).toBeNull();
    expect(localStorage.getItem('fkt_user')).toBeNull();
  });
});
