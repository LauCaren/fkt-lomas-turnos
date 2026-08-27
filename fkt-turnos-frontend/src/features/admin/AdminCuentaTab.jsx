import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { cambiarPasswordAdmin, limpiarMensajes } from './adminSlice';

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
      <h3 style={{ fontSize: 17, marginBottom: 14 }}>Cambiar contraseña</h3>
      {error && <div className="alert alert-error">{error}</div>}
      {mensajeExito && <div className="alert alert-success">{mensajeExito}</div>}
      <form onSubmit={handleSubmit} className="card">
        <div className="field">
          <label>Contraseña actual</label>
          <input type="password" required value={form.passwordActual} onChange={(e) => setForm({ ...form, passwordActual: e.target.value })} />
        </div>
        <div className="field">
          <label>Contraseña nueva</label>
          <input type="password" required minLength={6} value={form.passwordNueva} onChange={(e) => setForm({ ...form, passwordNueva: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-primary">Actualizar contraseña</button>
      </form>
    </div>
  );
}
