import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchMiPerfil, actualizarPerfil, cambiarPasswordCuenta, limpiarMensajesCuenta,
  iniciarMfaCuenta, activarMfaCuenta, desactivarMfaCuenta, fetchMisEvoluciones,
} from './cuentaSlice';
import { actualizarUsuarioLocal } from '../auth/authSlice';
import PasswordInput from '../../components/PasswordInput';
import SeccionMfa from '../../components/SeccionMfa';
import SeccionRevocarSesiones from '../../components/SeccionRevocarSesiones';
import SeccionPrivacidad from '../../components/SeccionPrivacidad';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../../constants/validacion';

const GENEROS = [
  { value: '', label: 'Seleccioná una opción' },
  { value: 'FEMENINO', label: 'Femenino' },
  { value: 'MASCULINO', label: 'Masculino' },
  { value: 'OTRO', label: 'Otro' },
  { value: 'PREFIERO_NO_DECIR', label: 'Prefiero no decir' },
];

const CANALES_NOTIFICACION = [
  { value: 'EMAIL', label: 'Email' },
  { value: 'WHATSAPP', label: 'WhatsApp' },
];

function DatosPersonales({ perfil }) {
  const dispatch = useDispatch();
  const [form, setForm] = useState({
    nombreCompleto: perfil.nombreCompleto,
    email: perfil.email,
    telefono: perfil.telefono || '',
    genero: perfil.genero || '',
    obraSocial: perfil.obraSocial || '',
    canalNotificacionPreferido: perfil.canalNotificacionPreferido || 'EMAIL',
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
      <div className="field">
        <label htmlFor="canalNotificacionPreferido">Cómo preferís recibir los recordatorios de turno</label>
        <select id="canalNotificacionPreferido" name="canalNotificacionPreferido" required value={form.canalNotificacionPreferido} onChange={handleChange}>
          {CANALES_NOTIFICACION.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
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
        <PasswordInput required value={form.passwordActual} onChange={(e) => setForm({ ...form, passwordActual: e.target.value })} />
      </div>
      <div className="field">
        <label>Contraseña nueva</label>
        <PasswordInput required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD} value={form.passwordNueva} onChange={(e) => setForm({ ...form, passwordNueva: e.target.value })} />
        <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 2 }}>{AYUDA_PASSWORD}</p>
      </div>
      <button type="submit" className="btn btn-primary">Actualizar contraseña</button>
    </form>
  );
}

function MiHistoriaClinica() {
  const dispatch = useDispatch();
  const evoluciones = useSelector((state) => state.cuenta.misEvoluciones);

  useEffect(() => {
    dispatch(fetchMisEvoluciones());
  }, [dispatch]);

  if (evoluciones.length === 0) {
    return <p style={{ color: 'var(--muted)' }}>Todavía no hay notas cargadas en tu historia clínica.</p>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      {evoluciones.map((e) => (
        <div key={e.id} className="card">
          <div style={{ fontWeight: 600 }}>{e.medicoNombre} · {e.especialidadNombre}</div>
          <div style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 8 }}>Turno del {e.fechaTurno}</div>
          <p style={{ fontSize: 14, whiteSpace: 'pre-wrap' }}>{e.texto}</p>
        </div>
      ))}
    </div>
  );
}

function Seccion({ titulo, full, children }) {
  return (
    <div className={full ? 'cuenta-full' : undefined}>
      <h2 style={{ fontSize: 18, marginBottom: 14 }}>{titulo}</h2>
      {children}
    </div>
  );
}

export default function MiCuentaPage() {
  const dispatch = useDispatch();
  const { perfil, mfaSetup, error, mensajeExito } = useSelector((state) => state.cuenta);

  useEffect(() => {
    dispatch(fetchMiPerfil());
    return () => dispatch(limpiarMensajesCuenta());
  }, [dispatch]);

  return (
    <div>
      <span className="eyebrow">Mi cuenta</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Tus datos</h1>

      {error && <div className="alert alert-error" style={{ marginTop: 20 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" style={{ marginTop: 20 }}>{mensajeExito}</div>}

      {!perfil && <p style={{ color: 'var(--muted)', marginTop: 20 }}>Cargando…</p>}

      {perfil && (
        <div className="cuenta-grid" style={{ marginTop: 24 }}>
          <Seccion titulo="Datos personales">
            <DatosPersonales key={perfil.id} perfil={perfil} />
          </Seccion>

          <Seccion titulo="Cambiar contraseña">
            <CambiarPassword />
          </Seccion>

          <Seccion titulo="Verificación en dos pasos">
            <SeccionMfa
              perfil={perfil}
              mfaSetup={mfaSetup}
              onIniciar={() => dispatch(iniciarMfaCuenta())}
              onActivar={(codigo) => dispatch(activarMfaCuenta(codigo)).unwrap().then(() => dispatch(fetchMiPerfil()))}
              onDesactivar={(codigo) => dispatch(desactivarMfaCuenta(codigo)).unwrap().then(() => dispatch(fetchMiPerfil()))}
            />
          </Seccion>

          <Seccion titulo="Sesiones activas">
            <SeccionRevocarSesiones />
          </Seccion>

          <Seccion titulo="Mi historia clínica" full>
            <MiHistoriaClinica />
          </Seccion>

          <Seccion titulo="Mis datos personales" full>
            <SeccionPrivacidad />
          </Seccion>
        </div>
      )}
    </div>
  );
}
