import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchMiPerfil, actualizarPerfil, cambiarPasswordCuenta, limpiarMensajesCuenta,
  iniciarMfaCuenta, activarMfaCuenta, desactivarMfaCuenta, fetchMisEvoluciones, fetchMisPagos,
} from './cuentaSlice';
import { actualizarUsuarioLocal } from '../auth/authSlice';
import PasswordInput from '../../components/PasswordInput';
import SeccionMfa from '../../components/SeccionMfa';
import SeccionRevocarSesiones from '../../components/SeccionRevocarSesiones';
import SeccionPrivacidad from '../../components/SeccionPrivacidad';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../../constants/validacion';
import { descargarArchivo } from '../../api/descargas';
import { formatMoneda } from '../../utils/formato';

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
  // Local, no en el slice: esta pantalla no necesita que ningún otro componente sepa que el
  // guardado está en curso, solo el propio botón.
  const [guardando, setGuardando] = useState(false);

  function handleChange(e) {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(limpiarMensajesCuenta());
    setGuardando(true);
    dispatch(actualizarPerfil(form)).unwrap().then((actualizado) => {
      dispatch(actualizarUsuarioLocal({ nombreCompleto: actualizado.nombreCompleto, email: actualizado.email }));
    }).catch(() => {}).finally(() => setGuardando(false));
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <div className="field">
        <label htmlFor="dni">DNI</label>
        <input id="dni" value={perfil.dni || ''} disabled />
        <p className="field-hint">El DNI no se puede cambiar. Si está mal, avisanos en el centro.</p>
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
      <button type="submit" className="btn btn-primary" disabled={guardando}>
        {guardando && <span className="spinner" aria-hidden="true" />}
        {guardando ? 'Guardando…' : 'Guardar cambios'}
      </button>
    </form>
  );
}

function CambiarPassword() {
  const dispatch = useDispatch();
  const [form, setForm] = useState({ passwordActual: '', passwordNueva: '' });
  const [actualizando, setActualizando] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(limpiarMensajesCuenta());
    setActualizando(true);
    dispatch(cambiarPasswordCuenta(form)).unwrap().then(() => {
      setForm({ passwordActual: '', passwordNueva: '' });
    }).catch(() => {}).finally(() => setActualizando(false));
  }

  return (
    <form onSubmit={handleSubmit} className="card">
      <div className="field">
        <label htmlFor="passwordActual">Contraseña actual</label>
        <PasswordInput id="passwordActual" required value={form.passwordActual} onChange={(e) => setForm({ ...form, passwordActual: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="passwordNueva">Contraseña nueva</label>
        <PasswordInput id="passwordNueva" required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD} value={form.passwordNueva} onChange={(e) => setForm({ ...form, passwordNueva: e.target.value })} />
        <p className="field-hint">{AYUDA_PASSWORD}</p>
      </div>
      <button type="submit" className="btn btn-primary" disabled={actualizando}>
        {actualizando && <span className="spinner" aria-hidden="true" />}
        {actualizando ? 'Actualizando…' : 'Actualizar contraseña'}
      </button>
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
    return <p className="cuenta-vacio">Todavía no hay notas cargadas en tu historia clínica.</p>;
  }

  return (
    <div className="card">
      <ol className="evolucion-timeline">
        {evoluciones.map((e) => (
          <li key={e.id} className="evolucion-item">
            <span className="evolucion-dot" aria-hidden="true" />
            <div className="evolucion-fecha">Turno del {e.fechaTurno} · {e.medicoNombre}, {e.especialidadNombre}</div>
            <p className="evolucion-texto">{e.texto}</p>
          </li>
        ))}
      </ol>
    </div>
  );
}

function MisPagos() {
  const dispatch = useDispatch();
  const pagos = useSelector((state) => state.cuenta.pagos);

  useEffect(() => {
    dispatch(fetchMisPagos());
  }, [dispatch]);

  if (pagos.length === 0) {
    return <p className="cuenta-vacio">Todavía no tenés pagos registrados.</p>;
  }

  return (
    <div className="card filas">
      {pagos.map((p) => (
        <div key={p.id} className="fila">
          <div className="fila-principal">
            <strong className="fila-monto">{formatMoneda(p.monto)}</strong>
            <span className="fila-detalle">{p.medicoNombre} · {new Date(p.fecha).toLocaleDateString('es-AR', { timeZone: 'UTC' })}</span>
            {p.anulado && <span className="badge badge-cancelado">Anulado</span>}
          </div>
          {!p.anulado && (
            <button
              type="button"
              className="btn btn-outline"
              onClick={() => descargarArchivo(`/api/mi-cuenta/pagos/${p.id}/comprobante`, null, `comprobante_${p.id}.pdf`)}
            >
              Descargar comprobante
            </button>
          )}
        </div>
      ))}
    </div>
  );
}

function Seccion({ titulo, full, children }) {
  return (
    <div className={full ? 'cuenta-full' : undefined}>
      <h2 className="cuenta-seccion-titulo">{titulo}</h2>
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
      <div className="page-head">
        <h1>Mi cuenta</h1>
        <p>Tus datos, tu seguridad y tu historia en el centro.</p>
      </div>

      {error && <div className="alert alert-error" role="alert" style={{ marginTop: 20 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginTop: 20 }}>{mensajeExito}</div>}

      {!perfil && (
        <div className="cuenta-grid" style={{ marginTop: 24 }}>
          <div className="card">
            <span className="skeleton skeleton-line" style={{ width: '40%' }} />
            <span className="skeleton skeleton-line" />
            <span className="skeleton skeleton-line" />
          </div>
          <div className="card">
            <span className="skeleton skeleton-line" style={{ width: '55%' }} />
            <span className="skeleton skeleton-line" />
          </div>
        </div>
      )}

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

          <Seccion titulo="Mis pagos" full>
            <MisPagos />
          </Seccion>

          <Seccion titulo="Mis datos personales" full>
            <SeccionPrivacidad />
          </Seccion>
        </div>
      )}
    </div>
  );
}
