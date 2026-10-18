import { useState } from 'react';
import ConsentimientoDatos from '../../components/ConsentimientoDatos';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { registrarPaciente } from './authSlice';
import PasswordInput from '../../components/PasswordInput';
import GoogleAuthButton from '../../components/GoogleAuthButton';
import { OBRA_SOCIAL_PARTICULAR } from '../../constants/obrasSociales';
import SelectorObraSocial from '../../components/SelectorObraSocial';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../../constants/validacion';
import { GOOGLE_CLIENT_ID } from '../../config';

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
    <div className="acceso acceso-ancho">
      <div className="acceso-cabecera">
        <h1>Creá tu cuenta</h1>
        <p>Con tu cuenta sacás, cambiás o cancelás turnos cuando quieras.</p>
      </div>

      <form onSubmit={handleSubmit} className="card acceso-hoja">
        {GOOGLE_CLIENT_ID && (
          <>
            <GoogleAuthButton />
            <div className="acceso-separador">o completá tus datos</div>
          </>
        )}
        {error && <div className="alert alert-error" role="alert">{error}</div>}

        <div className="field">
          <label htmlFor="nombreCompleto">Nombre y apellido</label>
          <input id="nombreCompleto" name="nombreCompleto" autoComplete="name" required value={form.nombreCompleto} onChange={handleChange} />
        </div>
        <div className="campos-2">
          <div className="field">
            <label htmlFor="dni">DNI</label>
            <input id="dni" name="dni" inputMode="numeric" required pattern="\d{7,8}" title="7 u 8 dígitos, sin puntos" value={form.dni} onChange={handleChange} />
            <p className="field-hint">Sin puntos</p>
          </div>
          <div className="field">
            <label htmlFor="telefono">Teléfono</label>
            <input id="telefono" name="telefono" type="tel" autoComplete="tel" required pattern="\+?[0-9\s\-\(\)]{6,20}" title="Solo números, espacios, guiones o paréntesis (6 a 20 caracteres)" value={form.telefono} onChange={handleChange} />
          </div>
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" autoComplete="email" required value={form.email} onChange={handleChange} />
        </div>
        <div className="campos-2">
          <div className="field">
            <label htmlFor="obraSocial">Obra social</label>
            <SelectorObraSocial id="obraSocial" name="obraSocial" value={form.obraSocial} onChange={handleChange} placeholder="Elegí una opción" />
          </div>
          <div className="field">
            <label htmlFor="genero">Género</label>
            <select id="genero" name="genero" required value={form.genero} onChange={handleChange}>
              {GENEROS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
            </select>
          </div>
        </div>
        {form.obraSocial === OBRA_SOCIAL_PARTICULAR && (
          <p className="reserva-aviso" style={{ marginTop: -4, marginBottom: 18 }}>
            Al no tener obra social, la sesión particular tiene un costo (a confirmar con el centro).
          </p>
        )}
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <PasswordInput id="password" name="password" required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD} value={form.password} onChange={handleChange} />
          <p className="field-hint">{AYUDA_PASSWORD}</p>
        </div>

        <div className="honeypot" aria-hidden="true">
          <label htmlFor="sitioWeb">No completar este campo</label>
          <input id="sitioWeb" name="sitioWeb" type="text" tabIndex={-1} autoComplete="off" value={form.sitioWeb} onChange={handleChange} />
        </div>

        <ConsentimientoDatos id="aceptaPoliticaPrivacidad" name="aceptaPoliticaPrivacidad" checked={form.aceptaPoliticaPrivacidad} onChange={handleChange} />

        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={status === 'loading'}>
          {status === 'loading' && <span className="spinner" aria-hidden="true" />}
          {status === 'loading' ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>

      <div className="acceso-pie">
        <p>¿Ya tenés cuenta? <Link to="/login" className="link-fuerte">Ingresá</Link></p>
      </div>
    </div>
  );
}
