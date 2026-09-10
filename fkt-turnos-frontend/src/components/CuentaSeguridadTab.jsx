import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchMiPerfil, cambiarPasswordCuenta, limpiarMensajesCuenta,
  iniciarMfaCuenta, activarMfaCuenta, desactivarMfaCuenta,
} from '../features/cuenta/cuentaSlice';
import PasswordInput from './PasswordInput';
import SeccionMfa from './SeccionMfa';
import SeccionRevocarSesiones from './SeccionRevocarSesiones';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../constants/validacion';

function CambiarPassword() {
  const dispatch = useDispatch();
  const [form, setForm] = useState({ passwordActual: '', passwordNueva: '' });

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(limpiarMensajesCuenta());
    dispatch(cambiarPasswordCuenta(form)).unwrap().then(() => {
      setForm({ passwordActual: '', passwordNueva: '' });
    }).catch(() => {});
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <div className="field">
        <label htmlFor="passwordActual">Contraseña actual</label>
        <PasswordInput id="passwordActual" required value={form.passwordActual} onChange={(e) => setForm({ ...form, passwordActual: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="passwordNueva">Contraseña nueva</label>
        <PasswordInput id="passwordNueva" required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD} value={form.passwordNueva} onChange={(e) => setForm({ ...form, passwordNueva: e.target.value })} />
        <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}>{AYUDA_PASSWORD}</p>
      </div>
      <button type="submit" className="btn btn-primary">Actualizar contraseña</button>
    </form>
  );
}

/** Cambio de contraseña + verificación en dos pasos — reutilizado por médico y contador, que
 * no tienen los campos de perfil de un paciente (obra social, etc.) pero sí necesitan poder
 * proteger su cuenta igual que el admin. El backend ya es genérico para cualquier rol logueado. */
export default function CuentaSeguridadTab() {
  const dispatch = useDispatch();
  const { perfil, mfaSetup, error, mensajeExito } = useSelector((state) => state.cuenta);

  useEffect(() => {
    dispatch(fetchMiPerfil());
    return () => dispatch(limpiarMensajesCuenta());
  }, [dispatch]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 20 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginBottom: 20 }}>{mensajeExito}</div>}

      {!perfil && (
        <div className="cuenta-grid">
          <div className="card">
            <span className="skeleton skeleton-line" style={{ width: '40%' }} />
            <span className="skeleton skeleton-line" />
            <span className="skeleton skeleton-line" />
          </div>
          <div className="card">
            <span className="skeleton skeleton-line" style={{ width: '55%' }} />
            <span className="skeleton skeleton-line" />
          </div>
        </div>
      )}

      {perfil && (
        <div className="cuenta-grid">
          <div>
            <h2 style={{ fontSize: 18, marginBottom: 16 }}>Cambiar contraseña</h2>
            <CambiarPassword />
          </div>

          <div>
            <h2 style={{ fontSize: 18, marginBottom: 16 }}>Verificación en dos pasos</h2>
            <SeccionMfa
              perfil={perfil}
              mfaSetup={mfaSetup}
              onIniciar={() => dispatch(iniciarMfaCuenta())}
              onActivar={(codigo) => dispatch(activarMfaCuenta(codigo)).unwrap().then(() => dispatch(fetchMiPerfil()))}
              onDesactivar={(codigo) => dispatch(desactivarMfaCuenta(codigo)).unwrap().then(() => dispatch(fetchMiPerfil()))}
            />
          </div>

          <div>
            <h2 style={{ fontSize: 18, marginBottom: 16 }}>Sesiones activas</h2>
            <SeccionRevocarSesiones />
          </div>
        </div>
      )}
    </div>
  );
}
