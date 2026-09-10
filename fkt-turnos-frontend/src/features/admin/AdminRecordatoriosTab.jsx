import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchRecordatorios, reenviarRecordatorio, fetchConfiguracionRecordatorios,
  actualizarConfiguracionRecordatorios, buscarPacientes, actualizarCanalPreferido, enviarMensajeManual,
  fetchMedicosAdmin, fetchPacientesEnRango, enviarMensajeMasivo,
} from './adminSlice';
import { hoyISO } from '../../utils/formato';
import EstadoVacio from '../../components/EstadoVacio';

function nombreCanal(valor) {
  return CANALES.find((c) => c.value === valor)?.label || valor;
}

const CANALES = [
  { value: 'EMAIL', label: 'Email' },
  { value: 'WHATSAPP', label: 'WhatsApp' },
];

const ESTADO_LABEL = { PENDIENTE: 'Pendiente', ENVIADO: 'Enviado', FALLIDO: 'Fallido' };

function ConfiguracionRecordatorios() {
  const dispatch = useDispatch();
  const configuracion = useSelector((state) => state.admin.configuracionRecordatorios);
  const [form, setForm] = useState({ plantillaMensaje: '', horasAnticipacion: 24 });

  useEffect(() => {
    dispatch(fetchConfiguracionRecordatorios());
  }, [dispatch]);

  useEffect(() => {
    if (configuracion) setForm(configuracion);
  }, [configuracion]);

  function handleGuardar(e) {
    e.preventDefault();
    dispatch(actualizarConfiguracionRecordatorios(form));
  }

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <h3 style={{ fontSize: 17, marginBottom: 14 }}>Configuración</h3>
      <form onSubmit={handleGuardar}>
        <div className="field">
          <label htmlFor="recordatoriosPlantilla">Mensaje del recordatorio</label>
          <textarea
            id="recordatoriosPlantilla"
            rows={4}
            value={form.plantillaMensaje}
            onChange={(e) => setForm({ ...form, plantillaMensaje: e.target.value })}
            style={{ width: '100%', padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6, fontFamily: 'inherit' }}
          />
          <span style={{ fontSize: 12.5, color: 'var(--muted)' }}>
            Placeholders disponibles: {'{paciente}'}, {'{profesional}'}, {'{especialidad}'}, {'{fecha}'}, {'{hora}'}
          </span>
          <div className="alert" style={{ marginTop: 10, background: 'var(--teal-tint)', color: 'var(--teal-dark)', border: '1px solid var(--teal)' }}>
            Este texto se usa solo para los recordatorios por <strong>WhatsApp</strong>. Los recordatorios por email
            usan un diseño fijo con la misma información del turno — cambiar este texto no afecta cómo se ven los emails.
          </div>
        </div>
        <div className="field">
          <label htmlFor="recordatoriosHorasAnticipacion">Con cuántas horas de anticipación avisar</label>
          <input
            id="recordatoriosHorasAnticipacion"
            type="number" min={1} max={168}
            value={form.horasAnticipacion}
            onChange={(e) => setForm({ ...form, horasAnticipacion: Number(e.target.value) })}
            style={{ width: 120 }}
          />
        </div>
        <button type="submit" className="btn btn-primary">Guardar configuración</button>
      </form>
    </div>
  );
}

function MensajePuntual() {
  const dispatch = useDispatch();
  const resultados = useSelector((state) => state.admin.resultadosBusquedaPacientes);
  const [busqueda, setBusqueda] = useState('');
  const [pacienteSeleccionado, setPacienteSeleccionado] = useState(null);
  const [canal, setCanal] = useState('EMAIL');
  const [canalPreferido, setCanalPreferido] = useState('EMAIL');
  const [mensaje, setMensaje] = useState('');

  function handleBuscar(e) {
    e.preventDefault();
    if (busqueda.trim()) dispatch(buscarPacientes(busqueda.trim()));
  }

  function handleSeleccionar(paciente) {
    setPacienteSeleccionado(paciente);
    setCanal(paciente.canalNotificacionPreferido);
    setCanalPreferido(paciente.canalNotificacionPreferido);
  }

  function handleGuardarCanalPreferido(e) {
    e.preventDefault();
    dispatch(actualizarCanalPreferido({ pacienteId: pacienteSeleccionado.id, canal: canalPreferido })).unwrap().then((actualizado) => {
      setPacienteSeleccionado(actualizado);
    }).catch(() => {});
  }

  function handleEnviar(e) {
    e.preventDefault();
    if (!pacienteSeleccionado || !mensaje.trim()) return;
    dispatch(enviarMensajeManual({ pacienteId: pacienteSeleccionado.id, canal, mensaje: mensaje.trim() })).unwrap().then(() => {
      setMensaje('');
    }).catch(() => {});
  }

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <h3 style={{ fontSize: 17, marginBottom: 14 }}>Mensaje puntual a un paciente</h3>

      <form onSubmit={handleBuscar} style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
        <input
          placeholder="Buscar por nombre o DNI"
          value={busqueda}
          onChange={(e) => setBusqueda(e.target.value)}
          style={{ flex: 1, padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6 }}
        />
        <button type="submit" className="btn btn-outline">Buscar</button>
      </form>

      {resultados.length > 0 && !pacienteSeleccionado && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 14 }}>
          {resultados.map((p) => (
            <button
              key={p.id}
              className="btn btn-outline"
              style={{ textAlign: 'left', padding: '8px 12px', fontSize: 13.5 }}
              onClick={() => handleSeleccionar(p)}
            >
              {p.nombreCompleto} — DNI {p.dni}
            </button>
          ))}
        </div>
      )}

      {pacienteSeleccionado && (
        <>
          <div style={{ marginBottom: 12, fontSize: 14 }}>
            Paciente: <strong>{pacienteSeleccionado.nombreCompleto}</strong>{' '}
            <button type="button" className="btn btn-outline" style={{ padding: '2px 10px', fontSize: 12 }} onClick={() => setPacienteSeleccionado(null)}>
              Cambiar
            </button>
          </div>

          <form onSubmit={handleGuardarCanalPreferido} style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginBottom: 20 }}>
            <div className="field" style={{ marginBottom: 0 }}>
              <label htmlFor="recordatoriosCanalPreferido">Canal preferido para los recordatorios automáticos</label>
              <select id="recordatoriosCanalPreferido" value={canalPreferido} onChange={(e) => setCanalPreferido(e.target.value)}>
                {CANALES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
            </div>
            <button type="submit" className="btn btn-outline">Guardar</button>
          </form>

          <form onSubmit={handleEnviar}>
            <div className="field">
              <label htmlFor="recordatoriosCanalMensaje">Canal del mensaje</label>
              <select id="recordatoriosCanalMensaje" value={canal} onChange={(e) => setCanal(e.target.value)}>
                {CANALES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
            </div>
            <div className="field">
              <label htmlFor="recordatoriosMensajeIndividual">Mensaje</label>
              <textarea
                id="recordatoriosMensajeIndividual"
                rows={3}
                maxLength={1600}
                value={mensaje}
                onChange={(e) => setMensaje(e.target.value)}
                style={{ width: '100%', padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6, fontFamily: 'inherit' }}
              />
            </div>
            <button type="submit" className="btn btn-primary">Enviar mensaje</button>
          </form>
        </>
      )}
    </div>
  );
}

function MensajeMasivo() {
  const dispatch = useDispatch();
  const medicos = useSelector((state) => state.admin.medicos);
  const pacientesEnRango = useSelector((state) => state.admin.pacientesEnRango);
  const pacientesEnRangoCargando = useSelector((state) => state.admin.pacientesEnRangoCargando);
  const [desde, setDesde] = useState(hoyISO());
  const [hasta, setHasta] = useState(hoyISO());
  const [medicoId, setMedicoId] = useState('');
  const [mensaje, setMensaje] = useState('');
  const [vistoPrevia, setVistoPrevia] = useState(false);

  useEffect(() => {
    if (medicos.length === 0) dispatch(fetchMedicosAdmin());
  }, [dispatch, medicos.length]);

  function handleVerDestinatarios(e) {
    e.preventDefault();
    dispatch(fetchPacientesEnRango({ desde, hasta, medicoId: medicoId || undefined })).unwrap().then(() => {
      setVistoPrevia(true);
    }).catch(() => {});
  }

  function handleEnviar(e) {
    e.preventDefault();
    if (!mensaje.trim() || pacientesEnRango.length === 0) return;
    dispatch(enviarMensajeMasivo({ desde, hasta, medicoId: medicoId || undefined, mensaje: mensaje.trim() })).unwrap().then(() => {
      setMensaje('');
      setVistoPrevia(false);
    }).catch(() => {});
  }

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <h3 style={{ fontSize: 17, marginBottom: 6 }}>Aviso masivo</h3>
      <p style={{ fontSize: 13.5, color: 'var(--muted)', marginBottom: 14 }}>
        Manda un mensaje a todos los pacientes con un turno reservado en un rango de fechas — por ejemplo, para avisar
        que el centro cierra un feriado. Cada uno lo recibe por su canal preferido (email o WhatsApp).
      </p>

      <form onSubmit={handleVerDestinatarios} style={{ display: 'flex', gap: 8, alignItems: 'flex-end', flexWrap: 'wrap', marginBottom: 14 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="masivoDesde">Desde</label>
          <input id="masivoDesde" type="date" value={desde} onChange={(e) => { setDesde(e.target.value); setVistoPrevia(false); }} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="masivoHasta">Hasta</label>
          <input id="masivoHasta" type="date" value={hasta} onChange={(e) => { setHasta(e.target.value); setVistoPrevia(false); }} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="masivoMedico">Médico (opcional)</label>
          <select id="masivoMedico" value={medicoId} onChange={(e) => { setMedicoId(e.target.value); setVistoPrevia(false); }}>
            <option value="">Todos</option>
            {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
          </select>
        </div>
        <button type="submit" className="btn btn-outline" disabled={pacientesEnRangoCargando}>
          {pacientesEnRangoCargando ? 'Buscando…' : 'Ver destinatarios'}
        </button>
      </form>

      {vistoPrevia && (
        pacientesEnRango.length === 0 ? (
          <EstadoVacio titulo="Sin destinatarios" subtitulo="No hay pacientes con turno reservado en ese rango." />
        ) : (
          <>
            <p style={{ fontSize: 13.5, marginBottom: 10 }}>
              Se va a enviar a <strong>{pacientesEnRango.length}</strong> paciente{pacientesEnRango.length === 1 ? '' : 's'}:{' '}
              <span style={{ color: 'var(--muted)' }}>{pacientesEnRango.map((p) => p.nombreCompleto).join(', ')}</span>
            </p>
            <form onSubmit={handleEnviar}>
              <div className="field">
                <label htmlFor="recordatoriosMensajeMasivo">Mensaje</label>
                <textarea
                  id="recordatoriosMensajeMasivo"
                  rows={3}
                  maxLength={1600}
                  value={mensaje}
                  onChange={(e) => setMensaje(e.target.value)}
                  style={{ width: '100%', padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6, fontFamily: 'inherit' }}
                />
              </div>
              <button type="submit" className="btn btn-primary">
                Enviar a {pacientesEnRango.length} paciente{pacientesEnRango.length === 1 ? '' : 's'}
              </button>
            </form>
          </>
        )
      )}
    </div>
  );
}

export default function AdminRecordatoriosTab() {
  const dispatch = useDispatch();
  const { recordatorios, error, mensajeExito } = useSelector((state) => state.admin);
  // La plantilla/anticipación es política del centro (ver AdminRecordatorioController,
  // /configuracion quedó ADMIN-only) — se oculta para secretaria en vez de mostrar un
  // formulario que le va a devolver 403 al guardar.
  const esAdmin = useSelector((state) => state.auth.usuario?.role) === 'ADMIN';

  useEffect(() => {
    dispatch(fetchRecordatorios());
  }, [dispatch]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert">{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status">{mensajeExito}</div>}

      {esAdmin && <ConfiguracionRecordatorios />}
      <MensajePuntual />
      <MensajeMasivo />

      <h3 style={{ fontSize: 17, marginBottom: 14 }}>Recordatorios</h3>
      {recordatorios.length === 0 && <EstadoVacio titulo="Sin recordatorios" subtitulo="No hay recordatorios para mostrar todavía." />}
      <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {recordatorios.map((r) => (
          <div key={r.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12 }}>
            <div>
              <div style={{ fontWeight: 600 }}>{r.pacienteNombre}</div>
              <div style={{ fontSize: 13, color: 'var(--muted)' }}>
                {r.fecha} {r.horaInicio?.slice(0, 5)} hs · {r.medicoNombre} ({r.especialidadNombre}) · {nombreCanal(r.canal)}
              </div>
              {r.errorMensaje && r.estado !== 'ENVIADO' && (
                <div style={{ fontSize: 12.5, color: 'var(--red)', marginTop: 4 }}>{r.errorMensaje}</div>
              )}
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <span style={{ fontSize: 13, fontWeight: 600 }}>{ESTADO_LABEL[r.estado]}</span>
              {r.estado !== 'ENVIADO' && (
                <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => dispatch(reenviarRecordatorio(r.id))}>
                  Reenviar
                </button>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
