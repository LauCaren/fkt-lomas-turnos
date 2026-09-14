import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { login, limpiarMensajesAuth } from './authSlice';
import PasswordInput from '../../components/PasswordInput';
import FluidOrb from '../../components/FluidOrb';
import GoogleAuthButton from '../../components/GoogleAuthButton';
import { RUTA_POR_ROL, GOOGLE_CLIENT_ID } from '../../config';

export default function LoginPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { status, error, requiereMfa, mfaOrigen } = useSelector((state) => state.auth);
  const requiereMfaPassword = requiereMfa && mfaOrigen === 'password';
  const [form, setForm] = useState({ email: '', password: '', codigoMfa: '' });

  // Solo se usa si es una ruta interna (nunca una URL absoluta) — evita un open-redirect
  // si alguien arma un link de login con un ?returnTo= apuntando a otro sitio.
  const returnTo = searchParams.get('returnTo');
  const destinoValido = returnTo && returnTo.startsWith('/') && !returnTo.startsWith('//');

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(login(form)).unwrap().then((data) => {
      navigate(destinoValido ? returnTo : (RUTA_POR_ROL[data.role] || '/turnos'));
    }).catch(() => {});
  }

  return (
    <div style={{ maxWidth: 420, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 4 }}>
        <FluidOrb size={96} />
      </div>
      <div className="page-head" style={{ textAlign: 'center' }}>
        <span className="eyebrow">Ingresar</span>
        <h1>Accedé a tu cuenta</h1>
        <p>Ingresá con tu email y contraseña para sacar o revisar tus turnos.</p>
      </div>

      <form onSubmit={handleSubmit} className="card" style={{ marginTop: 28 }}>
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" required disabled={requiereMfaPassword} value={form.email} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <PasswordInput id="password" name="password" required disabled={requiereMfaPassword} value={form.password} onChange={handleChange} />
        </div>
        {requiereMfaPassword && (
          <div className="field">
            <label htmlFor="codigoMfa">Código de verificación</label>
            <input id="codigoMfa" name="codigoMfa" inputMode="numeric" pattern="\d{6}" maxLength={6} autoFocus required
              placeholder="123456" value={form.codigoMfa} onChange={handleChange} />
            <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 4 }}>Abrí tu app de autenticación y escribí el código de 6 dígitos.</p>
          </div>
        )}
        <button type="submit" className="btn btn-primary btn-block" disabled={status === 'loading'}>
          {status === 'loading' && <span className="spinner" aria-hidden="true" />}
          {status === 'loading' ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>

      {GOOGLE_CLIENT_ID && !requiereMfaPassword && (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '18px 0', color: 'var(--muted)', fontSize: 13 }}>
            <span style={{ flex: 1, height: 1, background: 'var(--line)' }} />
            O
            <span style={{ flex: 1, height: 1, background: 'var(--line)' }} />
          </div>
          <GoogleAuthButton />
        </>
      )}

      <p style={{ marginTop: 18, textAlign: 'center', color: 'var(--muted)' }}>
        <Link to="/olvide-password" onClick={() => dispatch(limpiarMensajesAuth())} style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>¿Olvidaste tu contraseña?</Link>
      </p>
      <p style={{ marginTop: 8, textAlign: 'center', color: 'var(--muted)' }}>
        ¿No tenés cuenta? <Link to="/registro" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>Creá una acá</Link>
      </p>
    </div>
  );
}
