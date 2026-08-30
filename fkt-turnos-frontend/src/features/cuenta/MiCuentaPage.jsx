import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchMiPerfil, actualizarPerfil, cambiarPasswordCuenta, limpiarMensajesCuenta } from './cuentaSlice';
import { actualizarUsuarioLocal } from '../auth/authSlice';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';

const GENEROS = [
  { value: '', label: 'Seleccioná una opción' },
  { value: 'FEMENINO', label: 'Femenino' },
  { value: 'MASCULINO', label: 'Masculino' },
  { value: 'OTRO', label: 'Otro' },
  { value: 'PREFIERO_NO_DECIR', label: 'Prefiero no decir' },
];

function DatosPersonales({ perfil }) {
  const dispatch = useDispatch();
  const [form, setForm] = useState({
    nombreCompleto: perfil.nombreCompleto,
    email: perfil.email,
    telefono: perfil.telefono || '',
    genero: perfil.genero || '',
    obraSocial: perfil.obraSocial || '',
  });

  function handleChange(e) {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(limpiarMensajesCuenta());
    dispatch(actualizarPerfil(form)).unwrap().then((actualizado) => {
      dispatch(actualizarUsuarioLocal({ nombreCompleto: actualizado.nombreCompleto, email: actualizado.email }));
    }).catch(() => {});
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <div className="field">
        <label>DNI</label>
        <input value={perfil.dni || ''} disabled style={{ background: 'var(--bg-soft)', color: 'var(--muted)' }} />
      </div>
      <div className="field">
        <label htmlFor="nombreCompleto">Nombre completo</label>
        <input id="nombreCompleto" name="nombreCompleto" required value={form.nombreCompleto} onChange={handleChange} />
      </div>
      <div className="field">
        <label htmlFor="obraSocial">Obra social</label>
        <select id="obraSocial" name="obraSocial" required value={form.obraSocial} onChange={handleChange}>
          <option value="">Seleccioná tu obra social</option>
          <option value={OBRA_SOCIAL_PARTICULAR}>{OBRA_SOCIAL_PARTICULAR}</option>
          {OBRAS_SOCIALES.map((os) => <option key={os} value={os}>{os}</option>)}
          <option value={OBRA_SOCIAL_OTRA}>{OBRA_SOCIAL_OTRA}</option>
        </select>
      </div>
      <div className="field">
        <label htmlFor="email">Email</label>
        <input id="email" name="email" type="email" required value={form.email} onChange={handleChange} />
      </div>
      <div className="field">
        <label htmlFor="telefono">Teléfono</label>
        <input id="telefono" name="telefono" required value={form.telefono} onChange={handleChange} />
      </div>
      <div className="field">
        <label htmlFor="genero">Género</label>
        <select id="genero" name="genero" required value={form.genero} onChange={handleChange}>
          {GENEROS.map((g) => <option key={g.value} value={g.value}>{g.label}</option>)}
        </select>
      </div>
      <button type="submit" className="btn btn-primary">Guardar cambios</button>
    </form>
  );
}

function CambiarPassword() {
  const dispatch = useDispatch();
  const [form, setForm] = useState({ passwordActual: '', passwordNueva: '' });

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(limpiarMensajesCuenta());
    dispatch(cambiarPasswordCuenta(form)).unwrap().then(() => {
      setForm({ passwordActual: '', passwordNueva: '' });
    }).catch(() => {});
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <div className="field">
        <label>Contraseña actual</label>
        <input type="password" required value={form.passwordActual} onChange={(e) => setForm({ ...form, passwordActual: e.target.value })} />
      </div>
      <div className="field">
        <label>Contraseña nueva</label>
        <input type="password" required minLength={6} value={form.passwordNueva} onChange={(e) => setForm({ ...form, passwordNueva: e.target.value })} />
      </div>
      <button type="submit" className="btn btn-primary">Actualizar contraseña</button>
    </form>
  );
}

export default function MiCuentaPage() {
  const dispatch = useDispatch();
  const { perfil, error, mensajeExito } = useSelector((state) => state.cuenta);

  useEffect(() => {
    dispatch(fetchMiPerfil());
    return () => dispatch(limpiarMensajesCuenta());
  }, [dispatch]);

  return (
    <div style={{ maxWidth: 480, margin: '0 auto' }}>
      <span className="eyebrow">Mi cuenta</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Tus datos</h1>

      {error && <div className="alert alert-error" style={{ marginTop: 20 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" style={{ marginTop: 20 }}>{mensajeExito}</div>}

      {!perfil && <p style={{ color: 'var(--muted)', marginTop: 20 }}>Cargando…</p>}

      {perfil && (
        <>
          <div style={{ marginTop: 24 }}>
            <DatosPersonales key={perfil.id} perfil={perfil} />
          </div>
          <h2 style={{ fontSize: 20, marginTop: 32, marginBottom: 16 }}>Cambiar contraseña</h2>
          <CambiarPassword />
        </>
      )}
    </div>
  );
}
