import { Fragment, useEffect, useMemo, useState } from 'react';
import { ArrowLeft, ArrowRight, Phone, UserCheck, UserX } from 'lucide-react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchAgenda, fetchFeriados, fetchMedicosAdmin, reservarParaPaciente, actualizarAsistencia, deshacerTurno,
  reprogramarTurno, cancelarTurnoStaff, cancelarTurnosLote, setDniPrellenado, setObjetivoReprogramar,
  fetchListaEsperaCompatibles, asignarTurnoAPaciente, enviarRecordatorioAhora,
} from './adminSlice';
import { fetchEspecialidades } from '../turnos/turnosSlice';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';
import { hoyISO } from '../../utils/formato';
import EstadoVacio from '../../components/EstadoVacio';
import { useConfirm } from '../../components/ui/useConfirm';
import { useToast } from '../../components/ui/useToast';
import axiosClient from '../../api/axiosClient';
import { leerPref, guardarPref } from '../../utils/uiPrefs';
import Goniometro from '../../components/ui/Goniometro';

function parseISO(fechaISO) {
  const [y, m, d] = fechaISO.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d));
}
function formatISO(date) {
  return date.toISOString().slice(0, 10);
}
function sumarDias(fechaISO, n) {
  const d = parseISO(fechaISO);
  d.setUTCDate(d.getUTCDate() + n);
  return formatISO(d);
}
function inicioSemana(fechaISO) {
  const dia = parseISO(fechaISO).getUTCDay();
  const diff = dia === 0 ? -6 : 1 - dia;
  return sumarDias(fechaISO, diff);
}
function etiquetaDia(fechaISO) {
  const d = parseISO(fechaISO);
  const texto = d.toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short', timeZone: 'UTC' });
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}

const PACIENTE_VACIO = { dni: '', nombreCompleto: '', telefono: '', obraSocial: '', email: '', motivoConsulta: '' };

const CONSULTA_MOBILE = '(max-width: 880px)';

// Punto 23 del pedido de UX: en mobile, las grillas de Día/Semana dejan de ser legibles con
// scroll horizontal — hay que pasar a una lista de tarjetas de verdad, no solo comprimir la
// grilla de escritorio. Mismo breakpoint que ya usa .sidebar-nav en index.css.
function useEsMobile() {
  const [esMobile, setEsMobile] = useState(() => window.matchMedia(CONSULTA_MOBILE).matches);
  useEffect(() => {
    const mq = window.matchMedia(CONSULTA_MOBILE);
    function handler(e) { setEsMobile(e.matches); }
    mq.addEventListener('change', handler);
    return () => mq.removeEventListener('change', handler);
  }, []);
  return esMobile;
}

function TarjetaAgendaMobile({ turno, etiqueta, onSeleccionar, activo }) {
  const disponible = turno.estado === 'DISPONIBLE';
  const confirmado = turno.confirmacionPrevia === 'CONFIRMADO';
  const ausente = turno.estadoAsistencia === 'AUSENTE';
  const estadoBadge = disponible ? 'disponible' : ausente ? 'ausente' : confirmado ? 'confirmado' : 'reservado';
  const estadoTexto = disponible ? 'Libre' : ausente ? 'Ausente' : confirmado ? 'Confirmado' : 'Reservado';
  return (
    <button
      type="button"
      className={`agenda-tarjeta${activo ? ' activa' : ''}`}
      aria-pressed={activo}
      onClick={() => onSeleccionar(turno.id)}
    >
      <span>{etiqueta} · <strong>{disponible ? 'Libre' : (turno.pacienteNombre || '').split(' ')[0]}</strong></span>
      <span className={`badge badge-${estadoBadge}`}>{estadoTexto}</span>
    </button>
  );
}

function FormularioReserva({ turno, onCancelar, dniInicial }) {
  const dispatch = useDispatch();
  const [datos, setDatos] = useState(() => (dniInicial ? { ...PACIENTE_VACIO, dni: dniInicial } : PACIENTE_VACIO));
  const [enviando, setEnviando] = useState(false);
  const [pacienteEncontrado, setPacienteEncontrado] = useState(null);
  const [buscandoDni, setBuscandoDni] = useState(false);

  // Se consume una sola vez: si el DNI llegó prellenado desde "Nuevo turno" en la ficha del
  // paciente, no debe quedar pegado para la próxima reserva por teléfono que abra cualquier
  // secretaria en esta pantalla.
  useEffect(() => {
    if (dniInicial) dispatch(setDniPrellenado(null));
  }, [dniInicial, dispatch]);

  // Autocompletar por DNI (punto 7 del pedido): evita que la secretaria tenga que volver a
  // preguntar nombre/teléfono si el paciente ya está registrado. Solo dispara con un DNI
  // completo (7 u 8 dígitos, mismo patrón que valida el backend) y con un debounce para no
  // pegarle a la API en cada tecla.
  useEffect(() => {
    const dni = datos.dni.trim();
    if (!/^\d{7,8}$/.test(dni)) {
      setPacienteEncontrado(null);
      return undefined;
    }
    setBuscandoDni(true);
    const timer = setTimeout(() => {
      axiosClient.get('/api/admin/pacientes/buscar', { params: { q: dni } })
        .then(({ data }) => {
          const match = data.find((p) => p.dni === dni);
          setPacienteEncontrado(match || null);
          if (match) {
            setDatos((prev) => ({ ...prev, nombreCompleto: match.nombreCompleto, telefono: match.telefono }));
          }
        })
        .catch(() => setPacienteEncontrado(null))
        .finally(() => setBuscandoDni(false));
    }, 400);
    return () => clearTimeout(timer);
  }, [datos.dni]);

  function handleSubmit(e) {
    e.preventDefault();
    setEnviando(true);
    dispatch(reservarParaPaciente({ turnoId: turno.id, payload: datos })).unwrap()
      .then(() => onCancelar())
      .catch(() => setEnviando(false));
  }

  return (
    <form onSubmit={handleSubmit} className="agenda-reserva">
      <h4 className="agenda-reserva-titulo">Reservar por teléfono</h4>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
          <label htmlFor="agendaReservaDni">DNI</label>
          <input id="agendaReservaDni" inputMode="numeric" autoFocus required value={datos.dni} onChange={(e) => setDatos({ ...datos, dni: e.target.value })} placeholder="30123456" />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '2 1 220px' }}>
          <label htmlFor="agendaReservaNombre">Nombre completo</label>
          <input id="agendaReservaNombre" required value={datos.nombreCompleto} onChange={(e) => setDatos({ ...datos, nombreCompleto: e.target.value })} />
        </div>
      </div>
      {buscandoDni && <p className="field-hint" style={{ margin: 0 }}>Buscando paciente…</p>}
      {!buscandoDni && pacienteEncontrado && (
        <p className="agenda-encontrado">
          Paciente registrado: completamos sus datos solos.
        </p>
      )}
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
          <label htmlFor="agendaReservaTelefono">Teléfono</label>
          <input id="agendaReservaTelefono" required value={datos.telefono} onChange={(e) => setDatos({ ...datos, telefono: e.target.value })} placeholder="11 5950-5888" />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
          <label htmlFor="agendaReservaObraSocial">Obra social</label>
          <select id="agendaReservaObraSocial" required value={datos.obraSocial} onChange={(e) => setDatos({ ...datos, obraSocial: e.target.value })}>
            <option value="">Seleccioná una obra social</option>
            <option value={OBRA_SOCIAL_PARTICULAR}>{OBRA_SOCIAL_PARTICULAR}</option>
            {OBRAS_SOCIALES.map((os) => <option key={os} value={os}>{os}</option>)}
            <option value={OBRA_SOCIAL_OTRA}>{OBRA_SOCIAL_OTRA}</option>
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 200px' }}>
          <label htmlFor="agendaReservaEmail">Email (opcional)</label>
          <input id="agendaReservaEmail" type="email" value={datos.email} onChange={(e) => setDatos({ ...datos, email: e.target.value })} />
        </div>
      </div>
      <div className="field" style={{ marginBottom: 0 }}>
        <label htmlFor="agendaReservaMotivo">Motivo de consulta (opcional)</label>
        <input id="agendaReservaMotivo" value={datos.motivoConsulta} onChange={(e) => setDatos({ ...datos, motivoConsulta: e.target.value })} />
      </div>
      <p className="field-hint">Si el DNI ya está registrado, se usa esa cuenta y se ignoran los demás datos.</p>
      <div style={{ display: 'flex', gap: 8 }}>
        <button type="submit" className="btn btn-primary" disabled={enviando}>
          {enviando && <span className="spinner" aria-hidden="true" />}
          {enviando ? 'Reservando…' : 'Confirmar reserva'}
        </button>
        <button type="button" className="btn btn-outline" onClick={onCancelar}>Cancelar</button>
      </div>
    </form>
  );
}

function CabeceraTurno({ turno, children }) {
  return (
    <div className="agenda-detalle-cabecera">
      <span className="agenda-detalle-hora">{turno.horaInicio.slice(0, 5)}</span>
      <div>
        <p className="agenda-detalle-dia">{etiquetaDia(turno.fecha)} · hasta las {turno.horaFin.slice(0, 5)}</p>
        <p className="agenda-detalle-medico">{turno.medicoNombre}</p>
      </div>
      {children}
    </div>
  );
}

function DetalleTurno({ turno, onCerrar, agendaCompleta, dniPrellenado }) {
  if (turno.estado === 'DISPONIBLE') {
    return (
      <div className="card agenda-detalle">
        <CabeceraTurno turno={turno}>
          <span className="badge badge-disponible">Libre</span>
        </CabeceraTurno>
        <ListaEsperaParaSlot turno={turno} />
        <FormularioReserva turno={turno} onCancelar={onCerrar} dniInicial={dniPrellenado} />
      </div>
    );
  }
  return (
    <div className="card agenda-detalle">
      <CabeceraTurno turno={turno}>
        <button className="btn btn-ghost btn-sm" onClick={onCerrar}>Cerrar</button>
      </CabeceraTurno>
      <div className="agenda-paciente">
        <p className="agenda-paciente-nombre"><strong>{turno.pacienteNombre}</strong> · DNI {turno.pacienteDni}</p>
        <p className="agenda-paciente-datos">{turno.pacienteTelefono} · {turno.pacienteObraSocial}</p>
        {turno.motivoConsulta && <p className="agenda-paciente-datos">Motivo: {turno.motivoConsulta}</p>}
        <div className="agenda-marcas">
          {turno.confirmacionPrevia === 'CONFIRMADO' && <span className="badge badge-confirmado">Confirmó que viene</span>}
          <RecordatorioEnAgenda turno={turno} />
        </div>
      </div>
      <div className="agenda-detalle-acciones">
        <CheckInBotones turno={turno} />
        <div className="agenda-acciones-secundarias">
          <BotonReprogramar turno={turno} agendaCompleta={agendaCompleta} />
          <BotonCancelarTurno turno={turno} />
          <BotonDeshacer turno={turno} />
        </div>
      </div>
    </div>
  );
}

function LeyendaAgenda() {
  return (
    <ul className="agenda-leyenda" aria-label="Referencias de la grilla">
      <li><span className="agenda-muestra agenda-celda-disponible" />Libre</li>
      <li><span className="agenda-muestra agenda-celda-reservado" />Reservado</li>
      <li><span className="agenda-muestra agenda-celda-reservado agenda-celda-confirmado" />Confirmó que viene</li>
      <li><span className="agenda-muestra agenda-celda-reservado agenda-celda-ausente" />Ausente</li>
    </ul>
  );
}

function CeldaAgenda({ turno, feriado, onSeleccionar }) {
  if (feriado) return <div className="agenda-celda agenda-celda-feriado" />;
  if (!turno) return <div className="agenda-celda" />;
  const disponible = turno.estado === 'DISPONIBLE';
  const confirmado = turno.confirmacionPrevia === 'CONFIRMADO';
  const ausente = turno.estadoAsistencia === 'AUSENTE';
  return (
    <button
      type="button"
      className={`agenda-celda-btn ${disponible ? 'agenda-celda-disponible' : 'agenda-celda-reservado'}${confirmado ? ' agenda-celda-confirmado' : ''}${ausente ? ' agenda-celda-ausente' : ''}`}
      onClick={() => onSeleccionar(turno.id)}
      title={disponible ? 'Disponible' : `${turno.pacienteNombre} (${turno.pacienteDni})${confirmado ? ' · confirmó que viene' : ''}${ausente ? ' · ausente' : ''}`}
    >
      {disponible ? 'Libre' : (turno.pacienteNombre || '').split(' ')[0]}
    </button>
  );
}

function CheckInBotones({ turno }) {
  const dispatch = useDispatch();
  const asistencia = turno.estadoAsistencia || 'PENDIENTE';

  function marcar(estado) {
    dispatch(actualizarAsistencia({ turnoId: turno.id, estado: asistencia === estado ? 'PENDIENTE' : estado }));
  }

  return (
    <div className="asistencia" role="group" aria-label="Asistencia">
      <button type="button" className="asistencia-btn asistencia-llego" aria-pressed={asistencia === 'LLEGO'} onClick={() => marcar('LLEGO')}>
        <UserCheck size={17} aria-hidden="true" /> Llegó
      </button>
      <button type="button" className="asistencia-btn asistencia-ausente" aria-pressed={asistencia === 'AUSENTE'} onClick={() => marcar('AUSENTE')}>
        <UserX size={17} aria-hidden="true" /> Ausente
      </button>
      {asistencia === 'ATENDIDO' && <span className="badge badge-llego">Atendido</span>}
    </div>
  );
}

function BotonDeshacer({ turno }) {
  const dispatch = useDispatch();
  const confirmar = useConfirm();

  async function handleClick() {
    const ok = await confirmar({
      titulo: 'Deshacer última acción',
      detalle: 'Deshace la última reserva o cancelación registrada en este turno.',
      confirmarTexto: 'Deshacer',
    });
    if (ok) dispatch(deshacerTurno(turno.id));
  }

  return (
    <button
      className="btn btn-ghost btn-sm"
      title="Deshace la última reserva/cancelación registrada en este turno"
      onClick={handleClick}
    >
      Deshacer
    </button>
  );
}

function BotonCancelarTurno({ turno }) {
  const dispatch = useDispatch();
  const confirmar = useConfirm();
  const toast = useToast();

  async function handleClick() {
    const ok = await confirmar({
      titulo: 'Cancelar turno',
      detalle: `Vas a cancelar el turno de ${turno.pacienteNombre} del ${etiquetaDia(turno.fecha)} a las ${turno.horaInicio.slice(0, 5)}. Se le va a avisar por su canal preferido.`,
      confirmarTexto: 'Cancelar turno',
      peligroso: true,
    });
    if (!ok) return;
    dispatch(cancelarTurnoStaff(turno.id)).unwrap()
      .then(() => {
        toast('Turno cancelado', {
          accion: { texto: 'Deshacer', onClick: () => dispatch(deshacerTurno(turno.id)) },
        });
      })
      .catch(() => {});
  }

  return (
    <button className="btn btn-ghost btn-peligro btn-sm" onClick={handleClick}>
      Cancelar turno
    </button>
  );
}

function ListaEsperaParaSlot({ turno }) {
  const dispatch = useDispatch();
  const toast = useToast();
  const [abierto, setAbierto] = useState(false);
  const [cargando, setCargando] = useState(false);
  const [compatibles, setCompatibles] = useState(null);
  const [asignandoId, setAsignandoId] = useState(null);

  function handleAbrir() {
    setAbierto(true);
    if (compatibles !== null) return;
    setCargando(true);
    dispatch(fetchListaEsperaCompatibles({ medicoId: turno.medicoId, fecha: turno.fecha })).unwrap()
      .then((data) => setCompatibles(data))
      .catch(() => setCompatibles([]))
      .finally(() => setCargando(false));
  }

  function handleAsignar(pacienteId) {
    setAsignandoId(pacienteId);
    dispatch(asignarTurnoAPaciente({ turnoId: turno.id, pacienteId })).unwrap()
      .then(() => toast('Turno asignado correctamente'))
      .catch(() => setAsignandoId(null));
  }

  if (!abierto) {
    return (
      <button type="button" className="btn btn-outline btn-sm" style={{ marginTop: 6 }} onClick={handleAbrir}>
        ¿Hay alguien esperando este horario?
      </button>
    );
  }

  return (
    <div style={{ marginTop: 6 }}>
      {cargando && <p className="field-hint">Buscando en la lista de espera…</p>}
      {!cargando && compatibles?.length === 0 && (
        <p style={{ fontSize: 13.5, color: 'var(--muted)' }}>Nadie anotado en la lista de espera para este médico este día.</p>
      )}
      {!cargando && compatibles?.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          <p style={{ fontSize: 13.5, color: 'var(--teal-dark)', fontWeight: 600 }}>
            Hay {compatibles.length} paciente{compatibles.length === 1 ? '' : 's'} esperando este horario
          </p>
          {compatibles.map((p) => (
            <div key={p.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8, fontSize: 14 }}>
              <span>{p.pacienteNombre} · {p.pacienteTelefono}</span>
              <button
                type="button"
                className="btn btn-outline btn-sm"
                disabled={asignandoId === p.pacienteId}
                onClick={() => handleAsignar(p.pacienteId)}
              >
                Asignar
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function RecordatorioEnAgenda({ turno }) {
  const dispatch = useDispatch();
  const toast = useToast();
  const [enviando, setEnviando] = useState(false);

  if (turno.estadoRecordatorio === 'ENVIADO') {
    return <span className="badge badge-confirmado">Recordatorio enviado</span>;
  }

  function handleEnviar() {
    setEnviando(true);
    dispatch(enviarRecordatorioAhora(turno.id)).unwrap()
      .then((r) => toast(r.recordatorio.estado === 'ENVIADO' ? 'Recordatorio enviado' : 'No se pudo enviar por ningún canal', { tipo: r.recordatorio.estado === 'ENVIADO' ? 'success' : 'error' }))
      .catch(() => {})
      .finally(() => setEnviando(false));
  }

  return (
    <div style={{ marginTop: 6, display: 'flex', alignItems: 'center', gap: 8 }}>
      {turno.estadoRecordatorio === 'FALLIDO' && <span className="badge badge-ausente">Recordatorio falló</span>}
      <button type="button" className="btn btn-outline btn-sm" disabled={enviando} onClick={handleEnviar}>
        {enviando && <span className="spinner" aria-hidden="true" />}
        {enviando ? 'Enviando…' : 'Enviar recordatorio'}
      </button>
    </div>
  );
}

function BotonReprogramar({ turno, agendaCompleta }) {
  const dispatch = useDispatch();
  const toast = useToast();
  const [abierto, setAbierto] = useState(false);
  const [enviando, setEnviando] = useState(false);

  const destinosDisponibles = useMemo(() => {
    if (!abierto) return [];
    return agendaCompleta
      .filter((t) => t.estado === 'DISPONIBLE' && t.medicoId === turno.medicoId && t.id !== turno.id)
      .sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio));
  }, [abierto, agendaCompleta, turno.medicoId, turno.id]);

  function elegirDestino(destino) {
    setEnviando(true);
    dispatch(reprogramarTurno({ turnoOrigenId: turno.id, turnoDestinoId: destino.id })).unwrap()
      .then(() => {
        setAbierto(false);
        setEnviando(false);
        toast('Turno reprogramado correctamente', {
          accion: {
            texto: 'Deshacer',
            onClick: () => {
              dispatch(deshacerTurno(turno.id));
              dispatch(deshacerTurno(destino.id));
            },
          },
        });
      })
      .catch(() => setEnviando(false));
  }

  if (!abierto) {
    return (
      <button type="button" className="btn btn-outline btn-sm" onClick={() => setAbierto(true)}>
        Reprogramar
      </button>
    );
  }

  return (
    <div style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--line)', flex: '1 1 100%' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <strong style={{ fontSize: 14.5 }}>Elegí el horario nuevo</strong>
        <button type="button" className="btn btn-outline btn-sm" onClick={() => setAbierto(false)} disabled={enviando}>
          Cancelar
        </button>
      </div>
      {destinosDisponibles.length === 0 ? (
        <p style={{ fontSize: 14, color: 'var(--muted)' }}>
          No hay otros horarios libres de este médico cargados en este rango. Cambiá de día o semana para ver más opciones.
        </p>
      ) : (
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {destinosDisponibles.map((d) => (
            <button
              key={d.id}
              type="button"
              className="btn btn-outline btn-sm"
              disabled={enviando}
              onClick={() => elegirDestino(d)}
            >
              {etiquetaDia(d.fecha)} · {d.horaInicio.slice(0, 5)}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function VistaLista({ agenda, agendaCompleta, agendaCargando, medicoId, reservandoId, setReservandoId, dniPrellenado, onRecargar }) {
  // Selección múltiple para cancelar varios turnos de una (ej. un médico avisa que no viene un
  // día entero) — solo tiene sentido sobre turnos RESERVADO, así que un cambio de filtro/día
  // que ya no los muestre limpia la selección para no dejar ids "fantasma" seleccionados.
  const [seleccionados, setSeleccionados] = useState(() => new Set());
  const dispatch = useDispatch();
  const confirmar = useConfirm();
  const toast = useToast();
  const [cancelandoLote, setCancelandoLote] = useState(false);

  const idsReservadosVisibles = useMemo(() => agenda.filter((t) => t.estado === 'RESERVADO').map((t) => t.id), [agenda]);

  useEffect(() => {
    setSeleccionados((actual) => {
      const visibles = new Set(idsReservadosVisibles);
      const filtrado = new Set([...actual].filter((id) => visibles.has(id)));
      return filtrado.size === actual.size ? actual : filtrado;
    });
  }, [idsReservadosVisibles]);

  function alternarSeleccion(id) {
    setSeleccionados((actual) => {
      const nuevo = new Set(actual);
      if (nuevo.has(id)) nuevo.delete(id); else nuevo.add(id);
      return nuevo;
    });
  }

  async function handleCancelarSeleccionados() {
    const ids = [...seleccionados];
    const ok = await confirmar({
      titulo: `Cancelar ${ids.length} ${ids.length === 1 ? 'turno' : 'turnos'}`,
      detalle: `Se va a avisar a cada paciente por su canal preferido, igual que al cancelar uno solo.`,
      confirmarTexto: `Cancelar ${ids.length === 1 ? 'turno' : 'turnos'}`,
      peligroso: true,
    });
    if (!ok) return;
    setCancelandoLote(true);
    try {
      const resultado = await dispatch(cancelarTurnosLote(ids)).unwrap();
      setSeleccionados(new Set());
      onRecargar();
      if (resultado.errores.length === 0) {
        toast(`${resultado.cancelados.length} ${resultado.cancelados.length === 1 ? 'turno cancelado' : 'turnos cancelados'}`);
      } else {
        toast(`${resultado.cancelados.length} cancelados, ${resultado.errores.length} no se pudieron cancelar`, { tipo: 'error' });
      }
    } catch {
      // el thunk ya deja el mensaje en state.admin.error
    } finally {
      setCancelandoLote(false);
    }
  }

  return (
    <>
      {agendaCargando && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[0, 1, 2].map((i) => (
            <div key={i} className="agenda-fila">
              <span className="skeleton skeleton-line" style={{ width: '40%' }} />
              <span className="skeleton skeleton-line" style={{ width: '65%' }} />
            </div>
          ))}
        </div>
      )}
      {!agendaCargando && agenda.length === 0 && (
        <EstadoVacio titulo="Sin turnos generados" subtitulo={`No hay turnos generados para ese día${medicoId ? ' con ese médico' : ''}.`} />
      )}
      {seleccionados.size > 0 && (
        <div className="agenda-lote">
          <span>{seleccionados.size} {seleccionados.size === 1 ? 'turno seleccionado' : 'turnos seleccionados'}</span>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-outline btn-sm" onClick={() => setSeleccionados(new Set())} disabled={cancelandoLote}>
              Deseleccionar todo
            </button>
            <button className="btn btn-dark btn-sm" onClick={handleCancelarSeleccionados} disabled={cancelandoLote}>
              {cancelandoLote && <span className="spinner" aria-hidden="true" />}
              Cancelar seleccionados
            </button>
          </div>
        </div>
      )}
      <div className="agenda-lista">
        {agenda.map((t) => (
          <div key={t.id} className={`agenda-fila${t.estado === 'DISPONIBLE' ? ' libre' : ''}${seleccionados.has(t.id) ? ' seleccionada' : ''}`}>
            <div className="agenda-fila-cuerpo">
              <div className="agenda-fila-izq">
                {t.estado === 'RESERVADO' && (
                  <input
                    type="checkbox"
                    checked={seleccionados.has(t.id)}
                    onChange={() => alternarSeleccion(t.id)}
                    aria-label={`Seleccionar el turno de ${t.pacienteNombre}`}
                    className="agenda-check"
                  />
                )}
                <div>
                  <p className="agenda-fila-hora">{t.horaInicio.slice(0, 5)}<span> a {t.horaFin.slice(0, 5)}</span></p>
                  <p className="agenda-fila-medico">{t.medicoNombre} · {t.especialidadNombre}</p>
                  {t.estado === 'DISPONIBLE' && t.confirmacionPrevia === 'RECHAZADO' && (
                    <p className="agenda-nota agenda-nota-alerta">El paciente anterior avisó que no venía: horario libre otra vez</p>
                  )}
                  {t.estado === 'DISPONIBLE' && <ListaEsperaParaSlot turno={t} />}
                  {t.estado === 'RESERVADO' && (
                    <div className="agenda-paciente">
                      <p className="agenda-paciente-nombre"><strong>{t.pacienteNombre}</strong> · DNI {t.pacienteDni}</p>
                      <p className="agenda-paciente-datos">{t.pacienteTelefono} · {t.pacienteObraSocial}</p>
                      {t.motivoConsulta && <p className="agenda-paciente-datos">Motivo: {t.motivoConsulta}</p>}
                      <div className="agenda-marcas">
                        {t.confirmacionPrevia === 'CONFIRMADO' && <span className="badge badge-confirmado">Confirmó que viene</span>}
                        {t.tieneEvolucion && (
                          <span className="badge badge-llego" title="El médico cargó una evolución para esta consulta — el contenido es privado del profesional">Evolución cargada</span>
                        )}
                        <RecordatorioEnAgenda turno={t} />
                      </div>
                      <CheckInBotones turno={t} />
                    </div>
                  )}
                </div>
              </div>
              <div className="agenda-fila-acciones">
                <span className={`badge badge-${t.estado === 'DISPONIBLE' ? 'disponible' : 'reservado'}`}>
                  {t.estado === 'DISPONIBLE' ? 'Libre' : 'Reservado'}
                </span>
                {t.estado === 'DISPONIBLE' && reservandoId !== t.id && (
                  <button className="btn btn-primary btn-sm" onClick={() => setReservandoId(t.id)}>
                    <Phone size={16} aria-hidden="true" /> Reservar
                  </button>
                )}
                {t.estado === 'RESERVADO' && <BotonReprogramar turno={t} agendaCompleta={agendaCompleta} />}
                {t.estado === 'RESERVADO' && <BotonCancelarTurno turno={t} />}
                <BotonDeshacer turno={t} />
              </div>
            </div>
            {reservandoId === t.id && <FormularioReserva turno={t} onCancelar={() => setReservandoId(null)} dniInicial={dniPrellenado} />}
          </div>
        ))}
      </div>
    </>
  );
}

function VistaSemana({ agenda, agendaCompleta, agendaCargando, feriados, medicoId, medicos, fecha, denso, dniPrellenado }) {
  const esMobile = useEsMobile();
  const [seleccionadoId, setSeleccionadoId] = useState(null);
  // Deriva del array vivo (no guarda una copia del turno al clickear la celda) — así
  // el panel de detalle refleja al toque los cambios de check-in/deshacer, en vez de
  // quedarse con una foto vieja hasta que se cierra y se vuelve a abrir.
  const seleccionado = agenda.find((t) => t.id === seleccionadoId) || null;

  const dias = useMemo(() => {
    const lunes = inicioSemana(fecha);
    return Array.from({ length: 7 }, (_, i) => sumarDias(lunes, i));
  }, [fecha]);

  const feriadosPorDia = useMemo(() => {
    const mapa = {};
    feriados.forEach((f) => { mapa[f.fecha] = f; });
    return mapa;
  }, [feriados]);

  const horarios = useMemo(() => {
    const set = new Set(agenda.map((t) => t.horaInicio.slice(0, 5)));
    return Array.from(set).sort();
  }, [agenda]);

  const porDiaYHora = useMemo(() => {
    const mapa = {};
    agenda.forEach((t) => { mapa[`${t.fecha}_${t.horaInicio.slice(0, 5)}`] = t; });
    return mapa;
  }, [agenda]);

  const turnosOrdenados = useMemo(() => (
    [...agenda].sort((a, b) => (a.fecha + a.horaInicio).localeCompare(b.fecha + b.horaInicio))
  ), [agenda]);

  if (!medicoId) {
    return <EstadoVacio titulo="Elegí un médico" subtitulo="Seleccioná un médico arriba para ver la vista semanal en detalle." />;
  }

  if (!agendaCargando && horarios.length === 0) {
    return <EstadoVacio titulo="Sin turnos generados" subtitulo={`No hay turnos generados para ${medicos.find((m) => String(m.id) === String(medicoId))?.nombreCompleto} en esta semana.`} />;
  }

  return (
    <div>
      {esMobile ? (
        <div className="agenda-lista">
          {turnosOrdenados.map((t) => (
            <TarjetaAgendaMobile
              key={t.id}
              turno={t}
              etiqueta={`${etiquetaDia(t.fecha)} ${t.horaInicio.slice(0, 5)}`}
              activo={t.id === seleccionadoId}
              onSeleccionar={setSeleccionadoId}
            />
          ))}
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <div className={`agenda-grilla${denso ? ' agenda-densa' : ''}`} style={{ gridTemplateColumns: '64px repeat(7, minmax(96px, 1fr))', minWidth: 760 }}>
            <div className="agenda-grilla-esquina" />
            {dias.map((dia) => (
              <div key={dia} className={`agenda-grilla-cabecera${feriadosPorDia[dia] ? ' feriado' : ''}${dia === hoyISO() ? ' hoy' : ''}`}>
                {etiquetaDia(dia)}
                {feriadosPorDia[dia] && (
                  <div className="agenda-grilla-feriado">{feriadosPorDia[dia].nombre}</div>
                )}
              </div>
            ))}

            {horarios.map((hora) => (
              <Fragment key={hora}>
                <div className="agenda-grilla-hora">
                  {hora}
                </div>
                {dias.map((dia) => (
                  <CeldaAgenda
                    key={`${dia}-${hora}`}
                    turno={porDiaYHora[`${dia}_${hora}`]}
                    feriado={feriadosPorDia[dia]}
                    onSeleccionar={setSeleccionadoId}
                  />
                ))}
              </Fragment>
            ))}
          </div>
        </div>
      )}
      {!esMobile && <LeyendaAgenda />}

      {seleccionado && <DetalleTurno turno={seleccionado} onCerrar={() => setSeleccionadoId(null)} agendaCompleta={agendaCompleta} dniPrellenado={dniPrellenado} />}
    </div>
  );
}

function VistaDia({ agenda, agendaCompleta, agendaCargando, feriados, medicos, medicoId, fecha, denso, dniPrellenado }) {
  const esMobile = useEsMobile();
  const [seleccionadoId, setSeleccionadoId] = useState(null);
  const seleccionado = agenda.find((t) => t.id === seleccionadoId) || null;

  const medicosColumna = useMemo(() => {
    if (medicoId) return medicos.filter((m) => String(m.id) === medicoId);
    const idsConTurno = new Set(agenda.map((t) => t.medicoId));
    return medicos.filter((m) => idsConTurno.has(m.id));
  }, [medicos, medicoId, agenda]);

  const horarios = useMemo(() => {
    const set = new Set(agenda.map((t) => t.horaInicio.slice(0, 5)));
    return Array.from(set).sort();
  }, [agenda]);

  const porMedicoYHora = useMemo(() => {
    const mapa = {};
    agenda.forEach((t) => { mapa[`${t.medicoId}_${t.horaInicio.slice(0, 5)}`] = t; });
    return mapa;
  }, [agenda]);

  const feriadoDelDia = feriados.find((f) => f.fecha === fecha);

  const turnosOrdenados = useMemo(() => (
    [...agenda].sort((a, b) => (a.horaInicio + a.medicoNombre).localeCompare(b.horaInicio + b.medicoNombre))
  ), [agenda]);

  if (!agendaCargando && (horarios.length === 0 || medicosColumna.length === 0)) {
    return <EstadoVacio titulo="Sin turnos generados" subtitulo="No hay turnos generados para este día." />;
  }

  return (
    <div>
      {esMobile ? (
        <div className="agenda-lista">
          {turnosOrdenados.map((t) => (
            <TarjetaAgendaMobile
              key={t.id}
              turno={t}
              etiqueta={`${t.horaInicio.slice(0, 5)} · ${t.medicoNombre}`}
              activo={t.id === seleccionadoId}
              onSeleccionar={setSeleccionadoId}
            />
          ))}
        </div>
      ) : (
        <div style={{ overflowX: 'auto' }}>
          <div className={`agenda-grilla${denso ? ' agenda-densa' : ''}`} style={{ gridTemplateColumns: `64px repeat(${medicosColumna.length}, minmax(120px, 1fr))`, minWidth: 120 * medicosColumna.length + 64 }}>
            <div className="agenda-grilla-esquina" />
            {medicosColumna.map((m) => (
              <div key={m.id} className={`agenda-grilla-cabecera${feriadoDelDia ? ' feriado' : ''}`}>
                {m.nombreCompleto}
              </div>
            ))}

            {horarios.map((hora) => (
              <Fragment key={hora}>
                <div className="agenda-grilla-hora">
                  {hora}
                </div>
                {medicosColumna.map((m) => (
                  <CeldaAgenda
                    key={`${m.id}-${hora}`}
                    turno={porMedicoYHora[`${m.id}_${hora}`]}
                    feriado={feriadoDelDia}
                    onSeleccionar={setSeleccionadoId}
                  />
                ))}
              </Fragment>
            ))}
          </div>
        </div>
      )}
      {!esMobile && <LeyendaAgenda />}

      {seleccionado && <DetalleTurno turno={seleccionado} onCerrar={() => setSeleccionadoId(null)} agendaCompleta={agendaCompleta} dniPrellenado={dniPrellenado} />}
    </div>
  );
}

export default function AdminAgendaTab() {
  const dispatch = useDispatch();
  const { medicos, agenda, agendaCargando, feriados, error, mensajeExito, dniPrellenado, objetivoReprogramar } = useSelector((state) => state.admin);
  const especialidades = useSelector((state) => state.turnos.especialidades);
  const [fecha, setFecha] = useState(() => objetivoReprogramar?.fecha || hoyISO());
  const [especialidadId, setEspecialidadId] = useState(() => leerPref('agenda.especialidadId', ''));
  const [medicoId, setMedicoId] = useState(() => (
    objetivoReprogramar ? String(objetivoReprogramar.medicoId) : leerPref('agenda.medicoId', '')
  ));
  const [reservandoId, setReservandoId] = useState(null);
  const [vista, setVista] = useState(() => leerPref('agenda.vista', 'lista'));
  const [denso, setDenso] = useState(() => leerPref('agenda.denso', false));
  const [filtroEstado, setFiltroEstado] = useState('todos');

  const medicosFiltrados = useMemo(() => {
    if (!especialidadId) return medicos;
    return medicos.filter((m) => String(m.especialidadId) === especialidadId);
  }, [medicos, especialidadId]);

  const rango = useMemo(() => {
    if (vista === 'semana') {
      const lunes = inicioSemana(fecha);
      return { desde: lunes, hasta: sumarDias(lunes, 6) };
    }
    return { desde: fecha, hasta: fecha };
  }, [vista, fecha]);

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
    dispatch(fetchEspecialidades());
  }, [dispatch]);

  useEffect(() => {
    dispatch(fetchAgenda({ desde: rango.desde, hasta: rango.hasta, medicoId, especialidadId }));
    dispatch(fetchFeriados({ desde: rango.desde, hasta: rango.hasta }));
  }, [rango.desde, rango.hasta, medicoId, especialidadId, dispatch]);

  // Preferencias de pantalla (punto 22 del pedido de UX) — todo menos la fecha, que siempre
  // arranca en hoy: la secretaria no debería tener que reconfigurar vista/médico cada vez.
  useEffect(() => { guardarPref('agenda.vista', vista); }, [vista]);
  useEffect(() => { guardarPref('agenda.denso', denso); }, [denso]);
  useEffect(() => { guardarPref('agenda.medicoId', medicoId); }, [medicoId]);
  useEffect(() => { guardarPref('agenda.especialidadId', especialidadId); }, [especialidadId]);

  // Flechas para moverse de día (o de semana, en vista Semana) sin tocar el mouse — pensado
  // para secretaría que pasa horas mirando esta pantalla. Se ignora si el foco está en un
  // campo de texto/select para no interferir con el tipeo normal.
  useEffect(() => {
    function handleKeyDown(e) {
      const tag = document.activeElement?.tagName;
      if (tag === 'INPUT' || tag === 'SELECT' || tag === 'TEXTAREA') return;
      if (e.key === 'ArrowLeft') {
        setFecha((f) => sumarDias(f, vista === 'semana' ? -7 : -1));
      } else if (e.key === 'ArrowRight') {
        setFecha((f) => sumarDias(f, vista === 'semana' ? 7 : 1));
      } else if (e.key.toLowerCase() === 't') {
        setFecha(hoyISO());
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [vista]);

  function handleEspecialidadChange(nuevaEspecialidadId) {
    setEspecialidadId(nuevaEspecialidadId);
    if (medicoId) {
      const medicoActual = medicos.find((m) => String(m.id) === medicoId);
      if (!medicoActual || (nuevaEspecialidadId && String(medicoActual.especialidadId) !== nuevaEspecialidadId)) {
        setMedicoId('');
      }
    }
  }

  const feriadoDelDia = feriados.find((f) => f.fecha === fecha);

  // Indicadores accionables arriba de la agenda: cuentan sobre TODO lo cargado (el rango
  // completo, día o semana), independiente del filtro elegido, para que el contador de cada
  // chip no cambie cuando se clickea otro.
  const contadores = useMemo(() => {
    const reservados = agenda.filter((t) => t.estado === 'RESERVADO');
    return {
      total: agenda.length,
      disponibles: agenda.filter((t) => t.estado === 'DISPONIBLE').length,
      reservados: reservados.length,
      confirmados: reservados.filter((t) => t.confirmacionPrevia === 'CONFIRMADO').length,
      pendientesConfirmacion: reservados.filter((t) => t.confirmacionPrevia === 'SIN_RESPUESTA').length,
      llegaron: agenda.filter((t) => t.estadoAsistencia === 'LLEGO').length,
      ausentes: agenda.filter((t) => t.estadoAsistencia === 'AUSENTE').length,
    };
  }, [agenda]);

  const agendaFiltrada = useMemo(() => {
    switch (filtroEstado) {
      case 'disponible': return agenda.filter((t) => t.estado === 'DISPONIBLE');
      case 'reservado': return agenda.filter((t) => t.estado === 'RESERVADO');
      case 'confirmado': return agenda.filter((t) => t.estado === 'RESERVADO' && t.confirmacionPrevia === 'CONFIRMADO');
      case 'pendiente_confirmacion': return agenda.filter((t) => t.estado === 'RESERVADO' && t.confirmacionPrevia === 'SIN_RESPUESTA');
      case 'llego': return agenda.filter((t) => t.estadoAsistencia === 'LLEGO');
      case 'ausente': return agenda.filter((t) => t.estadoAsistencia === 'AUSENTE');
      default: return agenda;
    }
  }, [agenda, filtroEstado]);

  const chipsEstado = [
    { key: 'todos', label: 'Turnos', count: contadores.total, colorClass: '' },
    { key: 'disponible', label: 'Libres', count: contadores.disponibles, colorClass: 'badge-disponible' },
    { key: 'reservado', label: 'Reservados', count: contadores.reservados, colorClass: 'badge-reservado' },
    { key: 'confirmado', label: 'Confirmados', count: contadores.confirmados, colorClass: 'badge-confirmado' },
    { key: 'pendiente_confirmacion', label: 'Pend. confirmación', count: contadores.pendientesConfirmacion, colorClass: 'badge-pendiente' },
    { key: 'llego', label: 'Llegaron', count: contadores.llegaron, colorClass: 'badge-llego' },
    { key: 'ausente', label: 'Ausentes', count: contadores.ausentes, colorClass: 'badge-ausente' },
  ].filter((chip) => chip.key === 'todos' || chip.count > 0);

  const tiraFechas = useMemo(() => {
    if (vista === 'semana') return [];
    return Array.from({ length: 7 }, (_, i) => sumarDias(fecha, i - 3));
  }, [fecha, vista]);

  const resumenDia = useMemo(() => {
    if (vista === 'semana') return null;
    const reservados = agenda.filter((t) => t.estado === 'RESERVADO');
    const disponibles = agenda.filter((t) => t.estado === 'DISPONIBLE').length;
    const esHoy = fecha === hoyISO();
    const ahora = new Date();
    const proximos = reservados
      .filter((t) => !esHoy || new Date(`${t.fecha}T${t.horaInicio}`) >= ahora)
      .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio))
      .slice(0, 5);
    return { esHoy, reservadosTotal: reservados.length, disponibles, proximos };
  }, [agenda, vista, fecha]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert">{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status">{mensajeExito}</div>}
      {dniPrellenado && (
        <div className="reserva-aviso" style={{ marginTop: 0, marginBottom: 16 }}>
          Elegí un horario libre para reservarle un turno al paciente con DNI {dniPrellenado} — sus datos se van a completar solos.
        </div>
      )}
      {objetivoReprogramar && (
        <div className="reserva-aviso alert-cerrable" style={{ marginTop: 0, marginBottom: 16, alignItems: 'center' }}>
          <span>
            Buscá el turno de <strong>{objetivoReprogramar.pacienteNombre}</strong> del {etiquetaDia(objetivoReprogramar.fecha)} a las {objetivoReprogramar.horaInicio.slice(0, 5)} y tocá "Reprogramar".
          </span>
          <button type="button" className="btn btn-outline btn-sm" style={{ flexShrink: 0 }} onClick={() => dispatch(setObjetivoReprogramar(null))}>
            Entendido
          </button>
        </div>
      )}

      {resumenDia && !agendaCargando && (
        <div className="agenda-resumen">
          <div className="agenda-resumen-medidor">
            <Goniometro progreso={(resumenDia.reservadosTotal + resumenDia.disponibles) ? resumenDia.reservadosTotal / (resumenDia.reservadosTotal + resumenDia.disponibles) : 0} compacto />
          </div>
          <div className="agenda-resumen-cifras">
            <p className="agenda-resumen-dia">{resumenDia.esHoy ? 'Hoy' : etiquetaDia(fecha)}</p>
            <p className="agenda-resumen-total"><strong>{resumenDia.reservadosTotal}</strong> reservado{resumenDia.reservadosTotal === 1 ? '' : 's'}</p>
            <p className="agenda-resumen-libres">{resumenDia.disponibles} libre{resumenDia.disponibles === 1 ? '' : 's'}</p>
          </div>
          {resumenDia.proximos.length > 0 && (
            <div className="agenda-resumen-proximos">
              <p className="agenda-resumen-subtitulo">{resumenDia.esHoy ? 'Próximos turnos' : 'Turnos del día'}</p>
              <ol>
                {resumenDia.proximos.map((t) => (
                  <li key={t.id}>
                    <strong>{t.horaInicio.slice(0, 5)}</strong> {t.pacienteNombre} <span>· {t.medicoNombre}</span>
                  </li>
                ))}
              </ol>
            </div>
          )}
        </div>
      )}

      <div className="agenda-barra">
        <div className="tabnav agenda-vistas" role="tablist" aria-label="Vista de la agenda">
          {[['lista', 'Lista'], ['dia', 'Día'], ['semana', 'Semana']].map(([id, label]) => (
            <button key={id} type="button" role="tab" aria-selected={vista === id} className={`tabnav-btn${vista === id ? ' active' : ''}`} onClick={() => setVista(id)}>{label}</button>
          ))}
        </div>
        {(vista === 'dia' || vista === 'semana') && (
          <label className="check agenda-compacta" title="Filas más chicas para ver más turnos sin scrollear">
            <input type="checkbox" checked={!!denso} onChange={() => setDenso((d) => !d)} />
            <span>Vista compacta</span>
          </label>
        )}
        <p className="agenda-atajos">
          <kbd aria-label="Flecha izquierda"><ArrowLeft size={13} aria-hidden="true" /></kbd><kbd aria-label="Flecha derecha"><ArrowRight size={13} aria-hidden="true" /></kbd> {vista === 'semana' ? 'semana' : 'día'} · <kbd>T</kbd> hoy
        </p>
        <button type="button" className="btn btn-outline btn-sm" onClick={() => setFecha(hoyISO())} title="Volver a hoy (tecla T)">Hoy</button>
      </div>

      {tiraFechas.length > 0 && (
        <div className="agenda-tira">
          {tiraFechas.map((d) => {
            const esSeleccionado = d === fecha;
            const esHoy = d === hoyISO();
            return (
              <button
                key={d}
                type="button"
                className={`agenda-tira-dia${esHoy ? ' hoy' : ''}`}
                aria-pressed={esSeleccionado}
                onClick={() => setFecha(d)}
              >
                {esHoy ? 'Hoy' : etiquetaDia(d)}
              </button>
            );
          })}
        </div>
      )}

      <div className="filtros-estado" role="group" aria-label="Filtrar por estado">
        {chipsEstado.map((chip) => (
          <button
            key={chip.key}
            type="button"
            className={`filtro-chip ${chip.colorClass}`}
            aria-pressed={filtroEstado === chip.key}
            onClick={() => setFiltroEstado(chip.key)}
          >
            {chip.label} <strong>{chip.count}</strong>
          </button>
        ))}
      </div>

      <div className="filter-bar">
        <div className="field">
          <label htmlFor="agendaFecha">{vista === 'semana' ? 'Semana de' : 'Día'}</label>
          <input id="agendaFecha" type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="agendaEspecialidad">Especialidad</label>
          <select id="agendaEspecialidad" value={especialidadId} onChange={(e) => handleEspecialidadChange(e.target.value)}>
            <option value="">Todas las especialidades</option>
            {especialidades.map((esp) => <option key={esp.id} value={esp.id}>{esp.nombre}</option>)}
          </select>
        </div>
        <div className="field">
          <label htmlFor="agendaMedico">Médico</label>
          <select id="agendaMedico" value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
            <option value="">Todos los médicos</option>
            {medicosFiltrados.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
          </select>
        </div>
      </div>

      {vista !== 'semana' && feriadoDelDia && (
        <div className="alert alert-error">
          Feriado: {feriadoDelDia.nombre}
        </div>
      )}

      {filtroEstado !== 'todos' && !agendaCargando && agendaFiltrada.length === 0 && (
        <EstadoVacio
          titulo="Ningún turno coincide con este filtro"
          subtitulo="Probá con otro estado o volvé a ver todos los turnos."
          accion={<button type="button" className="btn btn-outline" onClick={() => setFiltroEstado('todos')}>Ver todos</button>}
        />
      )}
      {(filtroEstado === 'todos' || agendaFiltrada.length > 0) && (
        <>
          {vista === 'lista' && (
            <VistaLista agenda={agendaFiltrada} agendaCompleta={agenda} agendaCargando={agendaCargando} medicoId={medicoId} reservandoId={reservandoId} setReservandoId={setReservandoId} dniPrellenado={dniPrellenado}
              onRecargar={() => dispatch(fetchAgenda({ desde: rango.desde, hasta: rango.hasta, medicoId, especialidadId }))} />
          )}
          {vista === 'dia' && (
            <VistaDia agenda={agendaFiltrada} agendaCompleta={agenda} agendaCargando={agendaCargando} feriados={feriados} medicos={medicos} medicoId={medicoId} fecha={fecha} denso={denso} dniPrellenado={dniPrellenado} />
          )}
          {vista === 'semana' && (
            <VistaSemana agenda={agendaFiltrada} agendaCompleta={agenda} agendaCargando={agendaCargando} feriados={feriados} medicoId={medicoId} medicos={medicos} fecha={fecha} denso={denso} dniPrellenado={dniPrellenado} />
          )}
        </>
      )}
    </div>
  );
}
