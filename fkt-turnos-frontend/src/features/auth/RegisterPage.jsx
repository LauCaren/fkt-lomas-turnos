import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { registrarPaciente } from './authSlice';
import PasswordInput from '../../components/PasswordInput';
import GoogleAuthButton from '../../components/GoogleAuthButton';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../../constants/validacion';
import { SITE_URL, GOOGLE_CLIENT_ID } from '../../config';

const GENEROS = [
  { value: '', label: 'Seleccioná una opción' },
  { value: 'FEMENINO', label: 'Femenino' },
  { value: 'MASCULINO', label: 'Masculino' },
  { value: 'OTRO', label: 'Otro' },
  { value: 'PREFIERO_NO_DECIR', label: 'Prefiero no decir' },
];

const initialForm = {
  dni: '', obraSocial: '', nombreCompleto: '', email: '', telefono: '', genero: '', password: '',
  aceptaPoliticaPrivacidad: false, sitioWeb: '',
};

export default function RegisterPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { status, error } = useSelector((state) => state.auth);
  const [form, setForm] = useState(initialForm);

  function handleChange(e) {
    const { name, type, checked, value } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(registrarPaciente(form)).unwrap().then(() => {
      navigate('/turnos');
    }).catch(() => {});
  }

  return (
    <div style={{ maxWidth: 480, margin: '0 auto' }}>
      <div className="page-head">
        <span className="eyebrow">Crear cuenta</span>
        <h1>Registrate para sacar tu turno</h1>
        <p>Necesitamos estos datos para gestionar tu atención en el centro.</p>
      </div>

      {GOOGLE_CLIENT_ID && (
        <>
          <GoogleAuthButton />
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '18px 0', color: 'var(--muted)', fontSize: 13 }}>
            <span style={{ flex: 1, height: 1, background: 'var(--line)' }} />
            O completá el formulario
            <span style={{ flex: 1, height: 1, background: 'var(--line)' }} />
          </div>
        </>
      )}

      <form onSubmit={handleSubmit} className="card" style={{ marginTop: GOOGLE_CLIENT_ID ? 0 : 28 }}>
        {error && <div className="alert alert-error" role="alert">{error}</div>}

        <div className="field">
          <label htmlFor="nombreCompleto">Nombre completo</label>
          <input id="nombreCompleto" name="nombreCompleto" required value={form.nombreCompleto} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="dni">DNI</label>
          <input id="dni" name="dni" required pattern="\d{7,8}" title="7 u 8 dígitos, sin puntos" value={form.dni} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="obraSocial">Obra social</label>
          <select id="obraSocial" name="obraSocial" required value={form.obraSocial} onChange={handleChange}>
            <option value="">Seleccioná tu obra social</option>
            <option value={OBRA_SOCIAL_PARTICULAR}>{OBRA_SOCIAL_PARTICULAR}</option>
            {OBRAS_SOCIALES.map((os) => <option key={os} value={os}>{os}</option>)}
            <option value={OBRA_SOCIAL_OTRA}>{OBRA_SOCIAL_OTRA}</option>
          </select>
          {form.obraSocial === OBRA_SOCIAL_PARTICULAR && (
            <p style={{ fontSize: 13, color: 'var(--teal-dark)', marginTop: 2 }}>
              Al no tener obra social, la sesión particular tiene un costo (a confirmar con el centro).
            </p>
          )}
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" required value={form.email} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="telefono">Teléfono</label>
          <input id="telefono" name="telefono" required pattern="\+?[0-9\s\-\(\)]{6,20}" title="Solo números, espacios, guiones o paréntesis (6 a 20 caracteres)" value={form.telefono} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="genero">Género</label>
          <select id="genero" name="genero" required value={form.genero} onChange={handleChange}>
            {GENEROS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
          </select>
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <PasswordInput id="password" name="password" required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD} value={form.password} onChange={handleChange} />
          <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}>{AYUDA_PASSWORD}</p>
        </div>

        <div style={{ position: 'absolute', left: -9999, opacity: 0 }} aria-hidden="true">
          <label htmlFor="sitioWeb">No completar este campo</label>
          <input id="sitioWeb" name="sitioWeb" type="text" tabIndex={-1} autoComplete="off" value={form.sitioWeb} onChange={handleChange} />
        </div>

        <div className="field" style={{ flexDirection: 'row', alignItems: 'flex-start', gap: 8 }}>
          <input id="aceptaPoliticaPrivacidad" name="aceptaPoliticaPrivacidad" type="checkbox" required style={{ marginTop: 3 }} checked={form.aceptaPoliticaPrivacidad} onChange={handleChange} />
          <label htmlFor="aceptaPoliticaPrivacidad" style={{ fontWeight: 400 }}>
            Acepto la <a href={`${SITE_URL}/politica-de-privacidad.html`} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>política de privacidad</a> y los <a href={`${SITE_URL}/terminos-y-condiciones.html`} target="_blank" rel="noopener noreferrer" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>términos y condiciones</a>
          </label>
        </div>

        <button type="submit" className="btn btn-primary btn-block" disabled={status === 'loading'}>
          {status === 'loading' && <span className="spinner" aria-hidden="true" />}
          {status === 'loading' ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>

      <p style={{ marginTop: 18, textAlign: 'center', color: 'var(--muted)' }}>
        ¿Ya tenés cuenta? <Link to="/login" style={{ color: 'var(--teal-dark)', fontWeight: 600 }}>Ingresá acá</Link>
      </p>
    </div>
  );
}
