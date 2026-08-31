import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { cambiarPasswordAdmin, iniciarMfa, activarMfa, desactivarMfa, limpiarMensajes } from './adminSlice';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import PasswordInput from '../../components/PasswordInput';
import SeccionMfa from '../../components/SeccionMfa';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../../constants/validacion';

export default function AdminCuentaTab() {
  const dispatch = useDispatch();
  const { error, mensajeExito } = useSelector((state) => state.admin);
  const perfil = useSelector((state) => state.cuenta.perfil);
  const mfaSetup = useSelector((state) => state.admin.mfaSetup);
  const [form, setForm] = useState({ passwordActual: '', passwordNueva: '' });

  useEffect(() => {
    dispatch(fetchMiPerfil());
  }, [dispatch]);

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
          <PasswordInput required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD} value={form.passwordNueva} onChange={(e) => setForm({ ...form, passwordNueva: e.target.value })} />
          <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}>{AYUDA_PASSWORD}</p>
        </div>
        <button type="submit" className="btn btn-primary">Actualizar contraseña</button>
      </form>

      <h3 style={{ fontSize: 17, marginTop: 28, marginBottom: 14 }}>Verificación en dos pasos</h3>
      <SeccionMfa
        perfil={perfil}
        mfaSetup={mfaSetup}
        onIniciar={() => dispatch(iniciarMfa())}
        onActivar={(codigo) => dispatch(activarMfa(codigo)).unwrap().then(() => dispatch(fetchMiPerfil()))}
        onDesactivar={(codigo) => dispatch(desactivarMfa(codigo)).unwrap().then(() => dispatch(fetchMiPerfil()))}
      />
    </div>
  );
}
