import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { restablecerPassword } from './authSlice';
import PasswordInput from '../../components/PasswordInput';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../../constants/validacion';

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
      <div className="acceso">
        <div className="acceso-cabecera">
          <h1>Este link no sirve</h1>
          <p>Puede que esté incompleto o que ya se haya usado. Pedí uno nuevo y te llega en un momento.</p>
        </div>
        <div className="acceso-pie">
          <p><Link to="/olvide-password" className="btn btn-primary btn-lg">Pedir un link nuevo</Link></p>
        </div>
      </div>
    );
  }

  return (
    <div className="acceso">
      <div className="acceso-cabecera">
        <h1>Elegí una nueva contraseña</h1>
        <p>Es la que vas a usar para ingresar de ahora en más.</p>
      </div>

      <form onSubmit={handleSubmit} className="card acceso-hoja">
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        {mensajeExito && <div className="alert alert-success" role="status">{mensajeExito}</div>}
        <div className="field">
          <label htmlFor="passwordNueva">Contraseña nueva</label>
          <PasswordInput id="passwordNueva" required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD} value={passwordNueva} onChange={(e) => setPasswordNueva(e.target.value)} />
          <p className="field-hint">{AYUDA_PASSWORD}</p>
        </div>
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={status === 'loading'}>
          {status === 'loading' && <span className="spinner" aria-hidden="true" />}
          {status === 'loading' ? 'Guardando…' : 'Guardar contraseña'}
        </button>
      </form>
    </div>
  );
}
