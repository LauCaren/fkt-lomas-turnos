import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchSecretarias, crearSecretaria, activarCuentaStaff, desactivarCuentaStaff, revocarSesionesCuentaStaff,
} from './adminSlice';
import PasswordInput from '../../components/PasswordInput';
import { PATRON_PASSWORD, TITULO_PASSWORD, AYUDA_PASSWORD } from '../../constants/validacion';
import EstadoVacio from '../../components/EstadoVacio';
import { useConfirm } from '../../components/ui/useConfirm';

const VACIO = { nombreCompleto: '', email: '', password: '' };

function NuevaSecretaria() {
  const dispatch = useDispatch();
  const [form, setForm] = useState(VACIO);
  const [abierto, setAbierto] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(crearSecretaria(form)).unwrap().then(() => {
      setForm(VACIO);
      setAbierto(false);
    }).catch(() => {});
  }

  if (!abierto) {
    return <button className="btn btn-primary" onClick={() => setAbierto(true)}>Nueva cuenta de secretaria</button>;
  }

  return (
    <form onSubmit={handleSubmit} className="card" style={{ marginBottom: 20, maxWidth: 420 }}>
      <div className="field">
        <label htmlFor="secretariaNombreCompleto">Nombre completo</label>
        <input id="secretariaNombreCompleto" required value={form.nombreCompleto} onChange={(e) => setForm({ ...form, nombreCompleto: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="secretariaEmail">Email</label>
        <input id="secretariaEmail" type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
      </div>
      <div className="field">
        <label htmlFor="secretariaPassword">Contraseña</label>
        <PasswordInput id="secretariaPassword" required minLength={8} pattern={PATRON_PASSWORD} title={TITULO_PASSWORD}
          value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <p style={{ fontSize: 13.5, color: 'var(--muted)', marginTop: 2 }}>{AYUDA_PASSWORD}</p>
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <button type="submit" className="btn btn-primary">Crear cuenta</button>
        <button type="button" className="btn btn-outline" onClick={() => { setAbierto(false); setForm(VACIO); }}>Cancelar</button>
      </div>
    </form>
  );
}

function FilaSecretaria({ cuenta }) {
  const dispatch = useDispatch();
  const confirmar = useConfirm();

  async function handleDesactivar() {
    const ok = await confirmar({
      titulo: 'Desactivar cuenta',
      detalle: `Se desactiva la cuenta de ${cuenta.nombreCompleto} y se cierran sus sesiones activas.`,
      confirmarTexto: 'Desactivar',
      peligroso: true,
    });
    if (ok) dispatch(desactivarCuentaStaff(cuenta.id));
  }

  async function handleRevocarSesiones() {
    const ok = await confirmar({
      titulo: 'Cerrar sesiones',
      detalle: `Cierra todas las sesiones activas de ${cuenta.nombreCompleto}.`,
      confirmarTexto: 'Cerrar sesiones',
    });
    if (ok) dispatch(revocarSesionesCuentaStaff(cuenta.id));
  }

  return (
    <div className="card" style={{ marginBottom: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 10 }}>
      <div>
        <div style={{ fontWeight: 600 }}>{cuenta.nombreCompleto}</div>
        <div style={{ fontSize: 14, color: 'var(--muted)' }}>{cuenta.email}</div>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <span style={{ fontSize: 14, fontWeight: 600, color: cuenta.activo ? 'var(--teal-dark)' : 'var(--red)' }}>
          {cuenta.activo ? 'Activa' : 'Desactivada'}
        </span>
        {cuenta.activo ? (
          <button className="btn btn-outline btn-sm" onClick={handleDesactivar}>
            Desactivar
          </button>
        ) : (
          <button className="btn btn-outline btn-sm" onClick={() => dispatch(activarCuentaStaff(cuenta.id))}>
            Reactivar
          </button>
        )}
        <button className="btn btn-outline btn-sm" onClick={handleRevocarSesiones}>
          Cerrar sesiones
        </button>
      </div>
    </div>
  );
}

export default function AdminSecretariasTab() {
  const dispatch = useDispatch();
  const { secretarias, error, mensajeExito } = useSelector((state) => state.admin);

  useEffect(() => {
    dispatch(fetchSecretarias());
  }, [dispatch]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 14 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginBottom: 14 }}>{mensajeExito}</div>}

      <p style={{ fontSize: 14.5, color: 'var(--muted)', marginBottom: 16, maxWidth: 640 }}>
        Cada secretaria entra con su propia cuenta — ve agenda, reserva telefónica, mensajes,
        postulaciones y recordatorios, pero no puede tocar médicos, caja ni reportes financieros.
        Desactivar una cuenta cierra de inmediato cualquier sesión abierta con ella.
      </p>

      <div style={{ marginBottom: 20 }}>
        <NuevaSecretaria />
      </div>

      {secretarias.length === 0 && <EstadoVacio titulo="Sin cuentas de secretaria" subtitulo="Todavía no hay cuentas de secretaria creadas." />}
      <div className="stagger">
        {secretarias.map((s) => <FilaSecretaria key={s.id} cuenta={s} />)}
      </div>
    </div>
  );
}
