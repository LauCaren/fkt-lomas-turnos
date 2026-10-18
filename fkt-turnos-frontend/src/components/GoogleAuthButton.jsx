import { useState } from 'react';
import ConsentimientoDatos from './ConsentimientoDatos';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { GoogleLogin } from '@react-oauth/google';
import { loginConGoogle, limpiarMensajesAuth } from '../features/auth/authSlice';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../constants/obrasSociales';
import { RUTA_POR_ROL, GOOGLE_CLIENT_ID } from '../config';

const GENEROS = [
  { value: '', label: 'Seleccioná una opción' },
  { value: 'FEMENINO', label: 'Femenino' },
  { value: 'MASCULINO', label: 'Masculino' },
  { value: 'OTRO', label: 'Otro' },
  { value: 'PREFIERO_NO_DECIR', label: 'Prefiero no decir' },
];

const perfilInicial = { dni: '', obraSocial: '', telefono: '', genero: '', aceptaPoliticaPrivacidad: false, sitioWeb: '' };

/**
 * Botón de "Iniciar sesión con Google", compartido por LoginPage y RegisterPage — para
 * pacientes únicamente (el personal no tiene esta opción, ver el comentario de
 * AuthService.loginConGoogle). Un solo componente porque el ida-y-vuelta es el mismo en los
 * dos lugares: 1) el usuario clickea el botón real de Google, 2) si la cuenta es nueva, este
 * mismo componente pide dni/obraSocial/teléfono/género antes de crearla (ver
 * GoogleAuthResponse.requierePerfil en el backend — el DNI es inmutable una vez creado, así
 * que no se puede dejar para después), y 3) si la cuenta ya tenía 2FA activado (opcional para
 * pacientes), pide el código como cualquier login normal.
 */
export default function GoogleAuthButton({ destinoPorDefecto = '/turnos' }) {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { requiereMfa, mfaOrigen, googlePerfilPendiente } = useSelector((state) => state.auth);
  const requiereMfaGoogle = requiereMfa && mfaOrigen === 'google';
  const [credential, setCredential] = useState(null);
  const [perfil, setPerfil] = useState(perfilInicial);
  const [codigoMfa, setCodigoMfa] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [errorGoogle, setErrorGoogle] = useState('');

  if (!GOOGLE_CLIENT_ID) return null;

  function irSegunRol(payload) {
    navigate(RUTA_POR_ROL[payload.role] || destinoPorDefecto);
  }

  function enviar(datos) {
    setEnviando(true);
    dispatch(loginConGoogle(datos)).unwrap()
      .then((payload) => { if (!payload.requierePerfil) irSegunRol(payload); })
      .catch(() => {})
      .finally(() => setEnviando(false));
  }

  function iniciar(credentialResponse) {
    dispatch(limpiarMensajesAuth());
    setErrorGoogle('');
    setCredential(credentialResponse.credential);
    enviar({ credential: credentialResponse.credential });
  }

  if (requiereMfaGoogle && credential) {
    return (
      <form onSubmit={(e) => { e.preventDefault(); enviar({ credential, codigoMfa }); }} className="card" style={{ marginTop: 12 }}>
        <div className="field">
          <label htmlFor="googleCodigoMfa">Código de verificación</label>
          <input id="googleCodigoMfa" inputMode="numeric" pattern="\d{6}" maxLength={6} autoFocus required
            placeholder="123456" value={codigoMfa} onChange={(e) => setCodigoMfa(e.target.value)} />
          <p style={{ fontSize: 13.5, color: 'var(--muted)', marginTop: 4 }}>Abrí tu app de autenticación y escribí el código de 6 dígitos.</p>
        </div>
        <button type="submit" className="btn btn-primary btn-block" disabled={enviando}>
          {enviando && <span className="spinner" aria-hidden="true" />}
          {enviando ? 'Verificando…' : 'Continuar'}
        </button>
      </form>
    );
  }

  if (googlePerfilPendiente && credential) {
    return (
      <form
        onSubmit={(e) => { e.preventDefault(); enviar({ credential, ...perfil }); }}
        className="card"
        style={{ marginTop: 12 }}
      >
        <p style={{ fontSize: 14.5, color: 'var(--muted)', marginBottom: 4 }}>
          Ya verificamos tu cuenta de Google ({googlePerfilPendiente.email}). Nos faltan estos datos para terminar de crear tu cuenta:
        </p>
        <div className="field">
          <label htmlFor="googleDni">DNI</label>
          <input id="googleDni" required pattern="\d{7,8}" title="7 u 8 dígitos, sin puntos"
            value={perfil.dni} onChange={(e) => setPerfil({ ...perfil, dni: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="googleObraSocial">Obra social</label>
          <select id="googleObraSocial" required value={perfil.obraSocial} onChange={(e) => setPerfil({ ...perfil, obraSocial: e.target.value })}>
            <option value="">Seleccioná tu obra social</option>
            <option value={OBRA_SOCIAL_PARTICULAR}>{OBRA_SOCIAL_PARTICULAR}</option>
            {OBRAS_SOCIALES.map((os) => <option key={os} value={os}>{os}</option>)}
            <option value={OBRA_SOCIAL_OTRA}>{OBRA_SOCIAL_OTRA}</option>
          </select>
        </div>
        <div className="field">
          <label htmlFor="googleTelefono">Teléfono</label>
          <input id="googleTelefono" required pattern="\+?[0-9\s\-\(\)]{6,20}" title="Solo números, espacios, guiones o paréntesis (6 a 20 caracteres)"
            value={perfil.telefono} onChange={(e) => setPerfil({ ...perfil, telefono: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="googleGenero">Género</label>
          <select id="googleGenero" required value={perfil.genero} onChange={(e) => setPerfil({ ...perfil, genero: e.target.value })}>
            {GENEROS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
          </select>
        </div>
        <div className="honeypot" aria-hidden="true">
          <label htmlFor="googleSitioWeb">No completar este campo</label>
          <input id="googleSitioWeb" type="text" tabIndex={-1} autoComplete="off"
            value={perfil.sitioWeb} onChange={(e) => setPerfil({ ...perfil, sitioWeb: e.target.value })} />
        </div>
        <ConsentimientoDatos id="googleAceptaPoliticaPrivacidad" checked={perfil.aceptaPoliticaPrivacidad}
          onChange={(e) => setPerfil({ ...perfil, aceptaPoliticaPrivacidad: e.target.checked })} />
        <button type="submit" className="btn btn-primary btn-block" disabled={enviando}>
          {enviando && <span className="spinner" aria-hidden="true" />}
          {enviando ? 'Creando cuenta…' : 'Terminar de crear mi cuenta'}
        </button>
      </form>
    );
  }

  return (
    <div>
      {errorGoogle && <div className="alert alert-error" role="alert" style={{ marginBottom: 10 }}>{errorGoogle}</div>}
      <div style={{ display: 'flex', justifyContent: 'center' }}>
        <GoogleLogin
          onSuccess={iniciar}
          onError={() => setErrorGoogle('No pudimos conectar con Google. Probá de nuevo o usá tu email y contraseña.')}
          text="continue_with"
          locale="es"
        />
      </div>
    </div>
  );
}
