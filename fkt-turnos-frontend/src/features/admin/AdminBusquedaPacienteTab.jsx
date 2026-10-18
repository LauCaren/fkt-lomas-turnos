import { useEffect, useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { useDispatch, useSelector } from 'react-redux';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';
import { formatMoneda, hoyISO } from '../../utils/formato';
import EstadoVacio from '../../components/EstadoVacio';
import UsuarioAvatar from '../../components/UsuarioAvatar';
import { setDniPrellenado, setObjetivoReprogramar, setPacienteAAbrir, cancelarTurnoStaff, enviarMensajeManual } from './adminSlice';
import { useConfirm } from '../../components/ui/useConfirm';
import { useToast } from '../../components/ui/useToast';

const CANALES = [
  { value: 'EMAIL', label: 'Email' },
  { value: 'WHATSAPP', label: 'WhatsApp' },
];

const ESTADO_KINESIOLOGIA = {
  PENDIENTE_VALIDACION: 'Pendiente de revisión',
  VALIDADO: 'Activo',
  RECHAZADO: 'Rechazado',
  PAUSADO: 'Pausado',
  CANCELADO: 'Cancelado',
};

const BADGE_KINESIOLOGIA = {
  PENDIENTE_VALIDACION: 'badge-pendiente',
  VALIDADO: 'badge-disponible',
  RECHAZADO: 'badge-ausente',
  PAUSADO: 'badge-cancelado',
  CANCELADO: 'badge-cancelado',
};

function PanelContactar({ paciente, onCerrar }) {
  const dispatch = useDispatch();
  const toast = useToast();
  const [canal, setCanal] = useState(paciente.canalNotificacionPreferido || 'EMAIL');
  const [mensaje, setMensaje] = useState('');
  const [enviando, setEnviando] = useState(false);

  function handleEnviar(e) {
    e.preventDefault();
    if (!mensaje.trim()) return;
    setEnviando(true);
    dispatch(enviarMensajeManual({ pacienteId: paciente.id, canal, mensaje: mensaje.trim() })).unwrap()
      .then(() => {
        toast('Mensaje enviado correctamente');
        onCerrar();
      })
      .catch(() => setEnviando(false));
  }

  return (
    <form onSubmit={handleEnviar} className="card" style={{ marginTop: 12, display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div className="field" style={{ marginBottom: 0 }}>
        <label htmlFor="fichaContactarCanal">Canal</label>
        <select id="fichaContactarCanal" value={canal} onChange={(e) => setCanal(e.target.value)}>
          {CANALES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
        </select>
      </div>
      <div className="field" style={{ marginBottom: 0 }}>
        <label htmlFor="fichaContactarMensaje">Mensaje</label>
        <textarea
          id="fichaContactarMensaje"
          rows={3}
          maxLength={1600}
          value={mensaje}
          onChange={(e) => setMensaje(e.target.value)}
          style={{ width: '100%', padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6, fontFamily: 'inherit' }}
        />
      </div>
      <div style={{ display: 'flex', gap: 8 }}>
        <button type="submit" className="btn btn-primary" disabled={enviando}>
          {enviando && <span className="spinner" aria-hidden="true" />}
          {enviando ? 'Enviando…' : 'Enviar mensaje'}
        </button>
        <button type="button" className="btn btn-outline" onClick={onCerrar}>Cancelar</button>
      </div>
    </form>
  );
}

function AccionesRapidasFicha({ ficha, irAAgenda, onRecargar }) {
  const dispatch = useDispatch();
  const confirmar = useConfirm();
  const toast = useToast();
  const [contactando, setContactando] = useState(false);

  const proximoTurno = ficha.turnos
    .filter((t) => t.estado === 'RESERVADO' && t.fecha >= hoyISO())
    .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))[0];

  function handleNuevoTurno() {
    dispatch(setDniPrellenado(ficha.paciente.dni));
    irAAgenda?.();
  }

  function handleReprogramar() {
    dispatch(setObjetivoReprogramar({
      turnoId: proximoTurno.id,
      medicoId: proximoTurno.medicoId,
      fecha: proximoTurno.fecha,
      horaInicio: proximoTurno.horaInicio,
      pacienteNombre: ficha.paciente.nombreCompleto,
    }));
    irAAgenda?.();
  }

  async function handleCancelar() {
    const ok = await confirmar({
      titulo: 'Cancelar turno',
      detalle: `Vas a cancelar el turno de ${ficha.paciente.nombreCompleto} del ${proximoTurno.fecha} a las ${proximoTurno.horaInicio.slice(0, 5)}. Se le va a avisar por su canal preferido.`,
      confirmarTexto: 'Cancelar turno',
      peligroso: true,
    });
    if (!ok) return;
    dispatch(cancelarTurnoStaff(proximoTurno.id)).unwrap()
      .then(() => {
        toast('Turno cancelado correctamente');
        onRecargar?.();
      })
      .catch(() => {});
  }

  return (
    <div style={{ marginBottom: 20 }}>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <button type="button" className="btn btn-outline" onClick={handleNuevoTurno}>Nuevo turno</button>
        {proximoTurno && (
          <>
            <button type="button" className="btn btn-outline" onClick={handleReprogramar}>Reprogramar</button>
            <button type="button" className="btn btn-outline" onClick={handleCancelar}>Cancelar turno</button>
          </>
        )}
        <button type="button" className="btn btn-outline" onClick={() => setContactando((v) => !v)}>Contactar</button>
      </div>
      {proximoTurno && (
        <p style={{ fontSize: 13.5, color: 'var(--muted)', marginTop: 6 }}>
          Próximo turno: {proximoTurno.fecha} {proximoTurno.horaInicio.slice(0, 5)} hs con {proximoTurno.medicoNombre}
        </p>
      )}
      {contactando && <PanelContactar paciente={ficha.paciente} onCerrar={() => setContactando(false)} />}
    </div>
  );
}

function Ficha({ ficha, irAAgenda, onRecargar }) {
  return (
    <div style={{ marginTop: 24 }}>
      <div className="ficha-print-header">
        <strong>FKT Lomas</strong> · Ficha de paciente · Impreso el {new Date().toLocaleDateString('es-AR')}
      </div>
      <div className="card" style={{ marginBottom: 20 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 8 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <UsuarioAvatar
              fotoUrl={`/api/usuarios/${ficha.paciente.id}/foto`}
              tieneFoto={ficha.paciente.tieneFoto}
              avatarGenerico={ficha.paciente.avatarGenerico}
              nombreCompleto={ficha.paciente.nombreCompleto}
              size={48}
            />
            <div>
              <div style={{ fontWeight: 600, fontSize: 17 }}>{ficha.paciente.nombreCompleto}</div>
              <div style={{ fontSize: 14.5, color: 'var(--muted)', marginTop: 4 }}>
                DNI {ficha.paciente.dni} · {ficha.paciente.telefono} · avisos por {ficha.paciente.canalNotificacionPreferido}
              </div>
            </div>
          </div>
          {ficha.ausenciasCount > 0 && (
            <span className="badge badge-cancelado" title="Turnos reservados a los que no se presentó">
              {ficha.ausenciasCount} inasistencia{ficha.ausenciasCount === 1 ? '' : 's'}
            </span>
          )}
        </div>
      </div>

      <AccionesRapidasFicha ficha={ficha} irAAgenda={irAAgenda} onRecargar={onRecargar} />

      <h3 style={{ fontSize: 16, marginBottom: 10 }}>Turnos ({ficha.turnos.length})</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 24 }}>
        {ficha.turnos.length === 0 && <EstadoVacio titulo="Sin turnos" />}
        {ficha.turnos.slice(0, 30).map((t) => (
          <div key={t.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', fontSize: 14.5 }}>
            <span>{t.fecha} {t.horaInicio.slice(0, 5)} · {t.medicoNombre} ({t.especialidadNombre})</span>
            <span className={`badge badge-${t.estado === 'DISPONIBLE' ? 'disponible' : t.estado === 'RESERVADO' ? 'reservado' : 'cancelado'}`}>{t.estado}</span>
          </div>
        ))}
      </div>

      <h3 style={{ fontSize: 16, marginBottom: 10 }}>Planes de kinesiología ({ficha.planesKinesiologia.length})</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 24 }}>
        {ficha.planesKinesiologia.length === 0 && <EstadoVacio titulo="Sin planes" />}
        {ficha.planesKinesiologia.map((p) => (
          <div key={p.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', fontSize: 14.5 }}>
            <span>{p.medicoNombre} · {p.sesionesGeneradas} de {p.cantidadSesiones} sesiones</span>
            <span className={`badge ${BADGE_KINESIOLOGIA[p.estado] || 'badge-cancelado'}`}>{ESTADO_KINESIOLOGIA[p.estado] || p.estado}</span>
          </div>
        ))}
      </div>

      <h3 style={{ fontSize: 16, marginBottom: 10 }}>Cobros asociados ({ficha.ingresos.length})</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {ficha.ingresos.length === 0 && <EstadoVacio titulo="Sin cobros asociados" subtitulo="No hay cobros asociados a un turno de este paciente." />}
        {ficha.ingresos.map((i) => (
          <div key={i.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', fontSize: 14.5, opacity: i.anulado ? 0.55 : 1 }}>
            <span>{i.fecha} · {i.medicoNombre} · {i.metodoPago}</span>
            <strong>{formatMoneda(i.monto)}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function AdminBusquedaPacienteTab({ irAAgenda }) {
  const dispatch = useDispatch();
  const pacienteAAbrir = useSelector((state) => state.admin.pacienteAAbrir);
  const [busqueda, setBusqueda] = useState('');
  const [resultados, setResultados] = useState([]);
  const [ficha, setFicha] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

  // Paciente elegido desde la búsqueda global (Ctrl+K): abrir su ficha directo, sin obligar
  // a repetir la búsqueda acá.
  useEffect(() => {
    if (pacienteAAbrir) {
      verFicha(pacienteAAbrir);
      dispatch(setPacienteAAbrir(null));
    }
  }, [pacienteAAbrir, dispatch]);

  function handleBuscar(e) {
    e.preventDefault();
    if (!busqueda.trim()) return;
    setError(null);
    setFicha(null);
    setCargando(true);
    axiosClient.get('/api/admin/pacientes/buscar', { params: { q: busqueda.trim() } })
      .then(({ data }) => setResultados(data))
      .catch((err) => setError(mensajeDeError(err)))
      .finally(() => setCargando(false));
  }

  function verFicha(pacienteId) {
    setError(null);
    setCargando(true);
    axiosClient.get(`/api/admin/pacientes/${pacienteId}/ficha`)
      .then(({ data }) => setFicha(data))
      .catch((err) => setError(mensajeDeError(err)))
      .finally(() => setCargando(false));
  }

  return (
    <div>
      <p style={{ color: 'var(--muted)', marginBottom: 16 }}>
        Buscá por nombre o DNI para ver de una todos los turnos, el plan de kinesiología y los cobros de un paciente.
        No incluye historia clínica (es privada del médico).
      </p>
      <form onSubmit={handleBuscar} style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <input
          placeholder="Nombre o DNI"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          style={{ flex: 1, maxWidth: 360, padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6 }}
        />
        <button type="submit" className="btn btn-primary" disabled={cargando}>
          {cargando && <span className="spinner" aria-hidden="true" />}
          Buscar
        </button>
      </form>

      {error && <div className="alert alert-error" role="alert">{error}</div>}

      {!ficha && resultados.length > 0 && (
        <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {resultados.map((p) => (
            <button
              key={p.id}
              className="card card-hover"
              onClick={() => verFicha(p.id)}
              style={{ textAlign: 'left', padding: '12px 18px', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'space-between', border: 'none', width: '100%' }}
            >
              <span style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <UsuarioAvatar fotoUrl={`/api/usuarios/${p.id}/foto`} tieneFoto={p.tieneFoto} avatarGenerico={p.avatarGenerico} nombreCompleto={p.nombreCompleto} size={34} />
                <span><strong>{p.nombreCompleto}</strong> · DNI {p.dni}</span>
              </span>
              <span style={{ color: 'var(--muted)' }}>{p.telefono}</span>
            </button>
          ))}
        </div>
      )}

      {ficha && (
        <>
          <div style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
            <button className="btn btn-ghost btn-sm" onClick={() => setFicha(null)}><ArrowLeft size={17} aria-hidden="true" /> Volver a resultados</button>
            <button className="btn btn-outline" onClick={() => window.print()}>Imprimir ficha</button>
          </div>
          <div id="ficha-imprimir">
            <Ficha ficha={ficha} irAAgenda={irAAgenda} onRecargar={() => verFicha(ficha.paciente.id)} />
          </div>
        </>
      )}
    </div>
  );
}
