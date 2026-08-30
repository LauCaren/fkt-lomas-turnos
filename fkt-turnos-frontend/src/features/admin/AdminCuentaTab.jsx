import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { cambiarPasswordAdmin, iniciarMfa, activarMfa, desactivarMfa, limpiarMensajes } from './adminSlice';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import PasswordInput from '../../components/PasswordInput';

function SeccionMfa() {
  const dispatch = useDispatch();
  const perfil = useSelector((state) => state.cuenta.perfil);
  const mfaSetup = useSelector((state) => state.admin.mfaSetup);
  const [codigo, setCodigo] = useState('');

  useEffect(() => {
    dispatch(fetchMiPerfil());
  }, [dispatch]);

  function handleActivar(e) {
    e.preventDefault();
    dispatch(activarMfa(codigo)).unwrap().then(() => {
      setCodigo('');
      dispatch(fetchMiPerfil());
    }).catch(() => {});
  }

  function handleDesactivar(e) {
    e.preventDefault();
    dispatch(desactivarMfa(codigo)).unwrap().then(() => {
      setCodigo('');
      dispatch(fetchMiPerfil());
    }).catch(() => {});
  }

  if (!perfil) return null;

  if (perfil.mfaHabilitado) {
    return (
      <form onSubmit={handleDesactivar} className="card" style={{ marginTop: 20 }}>
        <p style={{ fontSize: 14, marginBottom: 12 }}>
          <span className="badge badge-disponible">Activada</span> La verificación en dos pasos está activa en tu cuenta.
        </p>
        <div className="field">
          <label>Código de tu app de autenticación (para desactivar)</label>
          <input inputMode="numeric" pattern="\d{6}" maxLength={6} required value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="123456" />
        </div>
        <button type="submit" className="btn btn-outline">Desactivar verificación en dos pasos</button>
      </form>
    );
  }

  if (mfaSetup) {
    return (
      <form onSubmit={handleActivar} className="card" style={{ marginTop: 20 }}>
        <p style={{ fontSize: 14, marginBottom: 10 }}>
          1. Abrí Google Authenticator (o cualquier app de códigos TOTP) y agregá una cuenta nueva "manual".<br />
          2. Cargá este código secreto:
        </p>
        <code style={{ display: 'block', background: 'var(--bg-soft)', padding: '10px 14px', borderRadius: 6, fontSize: 15, letterSpacing: 1, marginBottom: 14, wordBreak: 'break-all' }}>
          {mfaSetup.secreto}
        </code>
        <p style={{ fontSize: 14, marginBottom: 12 }}>3. Escribí el código de 6 dígitos que te muestra la app para confirmar:</p>
        <div className="field">
          <label>Código</label>
          <input inputMode="numeric" pattern="\d{6}" maxLength={6} required autoFocus value={codigo} onChange={(e) => setCodigo(e.target.value)} placeholder="123456" />
        </div>
        <button type="submit" className="btn btn-primary">Confirmar y activar</button>
      </form>
    );
  }

  return (
    <div className="card" style={{ marginTop: 20 }}>
      <p style={{ fontSize: 14, marginBottom: 14 }}>
        <span className="badge badge-cancelado">Desactivada</span> Agregá un paso extra al iniciar sesión con una app de autenticación (Google Authenticator, Authy, etc.).
      </p>
      <button className="btn btn-outline" onClick={() => dispatch(iniciarMfa())}>Activar verificación en dos pasos</button>
    </div>
  );
}

export default function AdminCuentaTab() {
  const dispatch = useDispatch();
  const { error, mensajeExito } = useSelector((state) => state.admin);
  const [form, setForm] = useState({ passwordActual: '', passwordNueva: '' });

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(limpiarMensajes());
    dispatch(cambiarPasswordAdmin(form)).unwrap().then(() => {
      setForm({ passwordActual: '', passwordNueva: '' });
    }).catch(() => {});
  }

  return (
    <div style={{ maxWidth: 420 }}>
      {error && <div className="alert alert-error">{error}</div>}
      {mensajeExito && <div className="alert alert-success">{mensajeExito}</div>}

      <h3 style={{ fontSize: 17, marginBottom: 14 }}>Cambiar contraseña</h3>
      <form onSubmit={handleSubmit} className="card">
        <div className="field">
          <label>Contraseña actual</label>
          <PasswordInput required value={form.passwordActual} onChange={(e) => setForm({ ...form, passwordActual: e.target.value })} />
        </div>
        <div className="field">
          <label>Contraseña nueva</label>
          <PasswordInput required minLength={6} value={form.passwordNueva} onChange={(e) => setForm({ ...form, passwordNueva: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-primary">Actualizar contraseña</button>
      </form>

      <h3 style={{ fontSize: 17, marginTop: 28, marginBottom: 14 }}>Verificación en dos pasos</h3>
      <SeccionMfa />
    </div>
  );
}
