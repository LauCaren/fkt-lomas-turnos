import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link } from 'react-router-dom';
import { olvidePassword } from './authSlice';

export default function OlvidePasswordPage() {
  const dispatch = useDispatch();
  const { status, error, mensajeExito } = useSelector((state) => state.auth);
  const [email, setEmail] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(olvidePassword(email));
  }

  return (
    <div style={{ maxWidth: 420, margin: '0 auto' }}>
      <span className="eyebrow">Recuperar acceso</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>¿Olvidaste tu contraseña?</h1>
      <p style={{ marginTop: 10, color: 'var(--muted)' }}>Ingresá tu email y te mandamos un link para elegir una nueva contraseña.</p>

      <form onSubmit={handleSubmit} className="card" style={{ marginTop: 28 }}>
        {error && <div className="alert alert-error">{error}</div>}
        {mensajeExito && <div className="alert alert-success">{mensajeExito}</div>}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={status === 'loading'}>
          {status === 'loading' ? 'Enviando…' : 'Enviar link de recuperación'}
        </button>
      </form>

      <p style={{ marginTop: 18, textAlign: 'center', color: 'var(--muted)' }}>
        <Link to="/login" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>Volver a ingresar</Link>
      </p>
    </div>
  );
}
