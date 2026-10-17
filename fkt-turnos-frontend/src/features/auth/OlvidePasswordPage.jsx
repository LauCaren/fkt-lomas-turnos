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
    <div className="acceso">
      <div className="acceso-cabecera">
        <h1>¿Olvidaste tu contraseña?</h1>
        <p>Escribí tu email y te mandamos un link para elegir una nueva.</p>
      </div>

      <form onSubmit={handleSubmit} className="card acceso-hoja">
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        {mensajeExito && <div className="alert alert-success" role="status">{mensajeExito}</div>}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" type="email" autoComplete="email" required value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={status === 'loading'}>
          {status === 'loading' && <span className="spinner" aria-hidden="true" />}
          {status === 'loading' ? 'Enviando…' : 'Enviarme el link'}
        </button>
      </form>

      <div className="acceso-pie">
        <p><Link to="/login" className="link-fuerte">Volver a ingresar</Link></p>
      </div>
    </div>
  );
}
