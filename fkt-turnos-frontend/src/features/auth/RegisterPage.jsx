import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { registrarPaciente } from './authSlice';

const GENEROS = [
  { value: '', label: 'Seleccioná una opción' },
  { value: 'FEMENINO', label: 'Femenino' },
  { value: 'MASCULINO', label: 'Masculino' },
  { value: 'OTRO', label: 'Otro' },
  { value: 'PREFIERO_NO_DECIR', label: 'Prefiero no decir' },
];

const initialForm = {
  dni: '', obraSocial: '', nombreCompleto: '', email: '', telefono: '', genero: '', password: '',
};

export default function RegisterPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { status, error } = useSelector((state) => state.auth);
  const [form, setForm] = useState(initialForm);

  function handleChange(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(registrarPaciente(form)).unwrap().then(() => {
      navigate('/turnos');
    }).catch(() => {});
  }

  return (
    <div style={{ maxWidth: 480, margin: '0 auto' }}>
      <span className="eyebrow">Crear cuenta</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Registrate para sacar tu turno</h1>
      <p style={{ marginTop: 10, color: 'var(--muted)' }}>Necesitamos estos datos para gestionar tu atención en el centro.</p>

      <form onSubmit={handleSubmit} className="card" style={{ marginTop: 28 }}>
        {error && <div className="alert alert-error">{error}</div>}

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
          <input id="obraSocial" name="obraSocial" required placeholder="Ej: OSDE, Swiss Medical, Particular" value={form.obraSocial} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" required value={form.email} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="telefono">Teléfono</label>
          <input id="telefono" name="telefono" required pattern="\+?[0-9\s\-()]{6,20}" title="Solo números, espacios, guiones o paréntesis (6 a 20 caracteres)" value={form.telefono} onChange={handleChange} />
        </div>
        <div className="field">
          <label htmlFor="genero">Género</label>
          <select id="genero" name="genero" required value={form.genero} onChange={handleChange}>
            {GENEROS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
          </select>
        </div>
        <div className="field">
          <label htmlFor="password">Contraseña</label>
          <input id="password" name="password" type="password" required minLength={6} value={form.password} onChange={handleChange} />
        </div>

        <button type="submit" className="btn btn-primary btn-block" disabled={status === 'loading'}>
          {status === 'loading' ? 'Creando cuenta…' : 'Crear cuenta'}
        </button>
      </form>

      <p style={{ marginTop: 18, textAlign: 'center', color: 'var(--muted)' }}>
        ¿Ya tenés cuenta? <Link to="/login" style={{ color: 'var(--orange-dark)', fontWeight: 600 }}>Ingresá acá</Link>
      </p>
    </div>
  );
}
