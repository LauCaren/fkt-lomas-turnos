import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { login, limpiarMensajesAuth } from './authSlice';
import PasswordInput from '../../components/PasswordInput';
import Goniometro from '../../components/ui/Goniometro';
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
    <div className="acceso">
      {/* La aguja barre una vez al entrar y avanza un tramo más al pedir el código de 2FA:
          el mismo lenguaje de "medición" que después guía la reserva de turnos. */}
      <div className="acceso-marca">
        <Goniometro progreso={requiereMfaPassword ? 0.85 : 0.62} compacto />
      </div>
      <div className="acceso-cabecera">
        <h1>{requiereMfaPassword ? 'Verificá que sos vos' : 'Ingresá a tu cuenta'}</h1>
        <p>
          {requiereMfaPassword
            ? 'Falta un paso: el código de tu app de autenticación.'
            : 'Para sacar, cambiar o cancelar tus turnos.'}
        </p>
      </div>

      <form onSubmit={handleSubmit} className="card acceso-hoja">
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" autoComplete="email" required disabled={requiereMfaPassword} value={form.email} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <PasswordInput id="password" name="password" required disabled={requiereMfaPassword} value={form.password} onChange={handleChange} />
        </div>
        {requiereMfaPassword && (
          <div className="field">
            <label htmlFor="codigoMfa">Código de verificación</label>
            <input id="codigoMfa" name="codigoMfa" inputMode="numeric" autoComplete="one-time-code" pattern="\d{6}" maxLength={6} autoFocus required
              placeholder="123456" value={form.codigoMfa} onChange={handleChange} />
            <p className="acceso-mfa-ayuda">Abrí tu app de autenticación y escribí el código de 6 dígitos.</p>
          </div>
        )}
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={status === 'loading'}>
          {status === 'loading' && <span className="spinner" aria-hidden="true" />}
          {status === 'loading' ? 'Ingresando…' : 'Ingresar'}
        </button>

        {GOOGLE_CLIENT_ID && !requiereMfaPassword && (
          <>
            <div className="acceso-separador">o</div>
            <GoogleAuthButton />
          </>
        )}
      </form>

      <div className="acceso-pie">
        <p>
          <Link to="/olvide-password" onClick={() => dispatch(limpiarMensajesAuth())} className="link-fuerte">¿Olvidaste tu contraseña?</Link>
        </p>
        <p>
          ¿Es tu primera vez? <Link to="/registro" className="link-fuerte">Creá tu cuenta</Link>
        </p>
      </div>
    </div>
  );
}
