import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { restablecerPassword } from './authSlice';

export default function RestablecerPasswordPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const { status, error, mensajeExito } = useSelector((state) => state.auth);
  const [passwordNueva, setPasswordNueva] = useState('');

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(restablecerPassword({ token, passwordNueva })).unwrap().then(() => {
      setTimeout(() => navigate('/login'), 1500);
    }).catch(() => {});
  }

  if (!token) {
    return (
      <div style={{ maxWidth: 420, margin: '0 auto' }}>
        <div className="alert alert-error">Este link no es válido. Pedí uno nuevo desde "¿Olvidaste tu contraseña?".</div>
        <p style={{ marginTop: 18, textAlign: 'center' }}>
          <Link to="/olvide-password" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>Pedir un link nuevo</Link>
        </p>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 420, margin: '0 auto' }}>
      <span className="eyebrow">Recuperar acceso</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Elegí una nueva contraseña</h1>

      <form onSubmit={handleSubmit} className="card" style={{ marginTop: 28 }}>
        {error && <div className="alert alert-error">{error}</div>}
        {mensajeExito && <div className="alert alert-success">{mensajeExito}</div>}
        <div className="field">
          <label htmlFor="passwordNueva">Contraseña nueva</label>
          <input id="passwordNueva" type="password" required minLength={6} value={passwordNueva} onChange={(e) => setPasswordNueva(e.target.value)} />
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={status === 'loading'}>
          {status === 'loading' ? 'Guardando…' : 'Guardar contraseña'}
        </button>
      </form>
    </div>
  );
}
