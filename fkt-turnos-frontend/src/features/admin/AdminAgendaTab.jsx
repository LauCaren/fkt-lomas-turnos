import { Fragment, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAgenda, fetchFeriados, fetchMedicosAdmin, reservarParaPaciente, actualizarAsistencia, deshacerTurno } from './adminSlice';
import { fetchEspecialidades } from '../turnos/turnosSlice';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';
import { hoyISO } from '../../utils/formato';
import EstadoVacio from '../../components/EstadoVacio';
import { useConfirm } from '../../components/ui/useConfirm';

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

function FormularioReserva({ turno, onCancelar }) {
  const dispatch = useDispatch();
  const [datos, setDatos] = useState(PACIENTE_VACIO);
  const [enviando, setEnviando] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    setEnviando(true);
    dispatch(reservarParaPaciente({ turnoId: turno.id, payload: datos })).unwrap()
      .then(() => onCancelar())
      .catch(() => setEnviando(false));
  }

  return (
    <form onSubmit={handleSubmit} style={{ marginTop: 12, paddingTop: 12, borderTop: '1px solid var(--line)', display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
          <label htmlFor="agendaReservaDni">DNI</label>
          <input id="agendaReservaDni" required value={datos.dni} onChange={(e) => setDatos({ ...datos, dni: e.target.value })} placeholder="30123456" />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '2 1 220px' }}>
          <label htmlFor="agendaReservaNombre">Nombre completo</label>
          <input id="agendaReservaNombre" required value={datos.nombreCompleto} onChange={(e) => setDatos({ ...datos, nombreCompleto: e.target.value })} />
        </div>
      </div>
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
      <p style={{ fontSize: 12.5, color: 'var(--muted)' }}>Si el DNI ya está registrado, se usa esa cuenta y se ignoran los demás datos.</p>
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

function DetalleTurno({ turno, onCerrar }) {
  if (turno.estado === 'DISPONIBLE') {
    return (
      <div className="card" style={{ marginTop: 16 }}>
        <div style={{ fontWeight: 600, marginBottom: 4 }}>
          {etiquetaDia(turno.fecha)} · {turno.horaInicio.slice(0, 5)} a {turno.horaFin.slice(0, 5)} · {turno.medicoNombre}
        </div>
        <span className="badge badge-disponible">Disponible</span>
        <FormularioReserva turno={turno} onCancelar={onCerrar} />
      </div>
    );
  }
  return (
    <div className="card" style={{ marginTop: 16 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div>
          <div style={{ fontWeight: 600, marginBottom: 4 }}>
            {etiquetaDia(turno.fecha)} · {turno.horaInicio.slice(0, 5)} a {turno.horaFin.slice(0, 5)} · {turno.medicoNombre}
          </div>
          <span className="badge badge-reservado">Reservado</span>
          <div style={{ marginTop: 10, fontSize: 14 }}>
            <div><strong>{turno.pacienteNombre}</strong> · DNI {turno.pacienteDni}</div>
            <div style={{ color: 'var(--muted)' }}>{turno.pacienteTelefono} · {turno.pacienteObraSocial}</div>
            {turno.motivoConsulta && <div style={{ color: 'var(--muted)', marginTop: 2 }}>Motivo: {turno.motivoConsulta}</div>}
            {turno.confirmacionPrevia === 'CONFIRMADO' && (
              <div style={{ fontSize: 12.5, color: 'var(--teal-dark)', marginTop: 4 }}>Confirmó que viene</div>
            )}
          </div>
          <CheckInBotones turno={turno} />
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'flex-end' }}>
          <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={onCerrar}>Cerrar</button>
          <BotonDeshacer turno={turno} />
        </div>
      </div>
    </div>
  );
}

function LeyendaAgenda() {
  return (
    <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', fontSize: 12, color: 'var(--muted)', marginTop: 10 }}>
      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ width: 12, height: 12, borderRadius: 3, background: 'var(--teal-tint)', display: 'inline-block' }} />
        Libre
      </span>
      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ width: 12, height: 12, borderRadius: 3, background: 'var(--teal)', display: 'inline-block' }} />
        Reservado
      </span>
      <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <span style={{ width: 12, height: 12, borderRadius: 3, background: 'var(--teal)', boxShadow: 'inset 0 0 0 2px var(--green)', display: 'inline-block' }} />
        Confirmó que viene
      </span>
    </div>
  );
}

function CeldaAgenda({ turno, feriado, onSeleccionar }) {
  if (feriado) return <div className="agenda-celda agenda-celda-feriado" />;
  if (!turno) return <div className="agenda-celda" />;
  const disponible = turno.estado === 'DISPONIBLE';
  const confirmado = turno.confirmacionPrevia === 'CONFIRMADO';
  return (
    <button
      type="button"
      className={`agenda-celda-btn ${disponible ? 'agenda-celda-disponible' : 'agenda-celda-reservado'}${confirmado ? ' agenda-celda-confirmado' : ''}`}
      onClick={() => onSeleccionar(turno.id)}
      title={disponible ? 'Disponible' : `${turno.pacienteNombre} (${turno.pacienteDni})${confirmado ? ' · confirmó que viene' : ''}`}
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
    <div style={{ display: 'flex', gap: 6, marginTop: 8 }}>
      <button
        type="button"
        className="btn"
        onClick={() => marcar('LLEGO')}
        style={{
          padding: '5px 12px', fontSize: 12.5,
          border: asistencia === 'LLEGO' ? '1.5px solid var(--teal)' : '1.5px solid var(--line)',
          background: asistencia === 'LLEGO' ? 'var(--teal-tint)' : 'var(--white)',
          color: 'var(--ink)',
        }}
      >
        {asistencia === 'LLEGO' ? '✓ Llegó' : 'Llegó'}
      </button>
      <button
        type="button"
        className="btn"
        onClick={() => marcar('AUSENTE')}
        style={{
          padding: '5px 12px', fontSize: 12.5,
          border: asistencia === 'AUSENTE' ? '1.5px solid var(--red)' : '1.5px solid var(--line)',
          background: asistencia === 'AUSENTE' ? 'var(--red-tint)' : 'var(--white)',
          color: 'var(--ink)',
        }}
      >
        {asistencia === 'AUSENTE' ? '✓ Ausente' : 'Ausente'}
      </button>
      {asistencia === 'ATENDIDO' && <span style={{ padding: '5px 10px', fontSize: 12.5, color: 'var(--teal-dark)', fontWeight: 600 }}>Atendido</span>}
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
      className="btn btn-outline"
      style={{ padding: '6px 14px', fontSize: 13 }}
      title="Deshace la última reserva/cancelación registrada en este turno"
      onClick={handleClick}
    >
      Deshacer
    </button>
  );
}

function VistaLista({ agenda, agendaCargando, medicoId, reservandoId, setReservandoId }) {
  return (
    <>
      {agendaCargando && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[0, 1, 2].map((i) => (
            <div key={i} className="card" style={{ padding: '16px 20px' }}>
              <span className="skeleton skeleton-line" style={{ width: '40%' }} />
              <span className="skeleton skeleton-line" style={{ width: '65%' }} />
            </div>
          ))}
        </div>
      )}
      {!agendaCargando && agenda.length === 0 && (
        <EstadoVacio titulo="Sin turnos generados" subtitulo={`No hay turnos generados para ese día${medicoId ? ' con ese médico' : ''}.`} />
      )}
      <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {agenda.map((t) => (
          <div key={t.id} className="card" style={{ padding: '16px 20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 15 }}>
                  {t.horaInicio.slice(0, 5)} a {t.horaFin.slice(0, 5)} · {t.medicoNombre}
                </div>
                <div style={{ fontSize: 13, color: 'var(--muted)' }}>{t.especialidadNombre}</div>
                {t.estado === 'DISPONIBLE' && t.confirmacionPrevia === 'RECHAZADO' && (
                  <div style={{ fontSize: 12.5, color: 'var(--red)', marginTop: 4 }}>
                    El paciente anterior avisó que no venía — horario libre otra vez
                  </div>
                )}
                {t.estado === 'RESERVADO' && (
                  <div style={{ marginTop: 8, fontSize: 14 }}>
                    <div><strong>{t.pacienteNombre}</strong> · DNI {t.pacienteDni}</div>
                    <div style={{ color: 'var(--muted)' }}>{t.pacienteTelefono} · {t.pacienteObraSocial}</div>
                    {t.motivoConsulta && <div style={{ color: 'var(--muted)', marginTop: 2 }}>Motivo: {t.motivoConsulta}</div>}
                    {t.confirmacionPrevia === 'CONFIRMADO' && (
                      <div style={{ fontSize: 12.5, color: 'var(--teal-dark)', marginTop: 4 }}>Confirmó que viene</div>
                    )}
                    {t.tieneEvolucion && (
                      <div style={{ fontSize: 12.5, color: 'var(--teal-dark)', marginTop: 4 }} title="El médico cargó una evolución para esta consulta — el contenido es privado del profesional">
                        Evolución cargada
                      </div>
                    )}
                    <CheckInBotones turno={t} />
                  </div>
                )}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <span className={`badge badge-${t.estado === 'DISPONIBLE' ? 'disponible' : 'reservado'}`}>
                  {t.estado === 'DISPONIBLE' ? 'Disponible' : 'Reservado'}
                </span>
                {t.estado === 'DISPONIBLE' && reservandoId !== t.id && (
                  <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setReservandoId(t.id)}>
                    Reservar por teléfono
                  </button>
                )}
                <BotonDeshacer turno={t} />
              </div>
            </div>
            {reservandoId === t.id && <FormularioReserva turno={t} onCancelar={() => setReservandoId(null)} />}
          </div>
        ))}
      </div>
    </>
  );
}

function VistaSemana({ agenda, agendaCargando, feriados, medicoId, medicos, fecha, denso }) {
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

  if (!medicoId) {
    return <EstadoVacio titulo="Elegí un médico" subtitulo="Seleccioná un médico arriba para ver la vista semanal en detalle." />;
  }

  if (!agendaCargando && horarios.length === 0) {
    return <EstadoVacio titulo="Sin turnos generados" subtitulo={`No hay turnos generados para ${medicos.find((m) => String(m.id) === String(medicoId))?.nombreCompleto} en esta semana.`} />;
  }

  return (
    <div>
      <div style={{ overflowX: 'auto' }}>
        <div className={denso ? 'agenda-densa' : undefined} style={{ display: 'grid', gridTemplateColumns: '64px repeat(7, minmax(96px, 1fr))', minWidth: 760, border: '1px solid var(--line)', borderRadius: 10, overflow: 'hidden' }}>
          <div style={{ background: 'var(--bg-soft)', borderBottom: '1px solid var(--line)' }} />
          {dias.map((dia) => (
            <div key={dia} style={{
              padding: '10px 6px', textAlign: 'center', fontSize: 12.5, fontWeight: 600,
              background: feriadosPorDia[dia] ? 'var(--red-tint)' : 'var(--bg-soft)',
              borderBottom: '1px solid var(--line)', borderLeft: '1px solid var(--line)',
            }}>
              {etiquetaDia(dia)}
              {feriadosPorDia[dia] && (
                <div style={{ fontSize: 10.5, fontWeight: 500, color: 'var(--red)', marginTop: 2 }}>{feriadosPorDia[dia].nombre}</div>
              )}
            </div>
          ))}

          {horarios.map((hora) => (
            <Fragment key={hora}>
              <div style={{ padding: '8px 6px', fontSize: 12, color: 'var(--muted)', borderTop: '1px solid var(--line)', textAlign: 'right' }}>
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
      <LeyendaAgenda />

      {seleccionado && <DetalleTurno turno={seleccionado} onCerrar={() => setSeleccionadoId(null)} />}
    </div>
  );
}

function VistaDia({ agenda, agendaCargando, feriados, medicos, medicoId, fecha, denso }) {
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

  if (!agendaCargando && (horarios.length === 0 || medicosColumna.length === 0)) {
    return <EstadoVacio titulo="Sin turnos generados" subtitulo="No hay turnos generados para este día." />;
  }

  return (
    <div>
      <div style={{ overflowX: 'auto' }}>
        <div className={denso ? 'agenda-densa' : undefined} style={{
          display: 'grid',
          gridTemplateColumns: `64px repeat(${medicosColumna.length}, minmax(120px, 1fr))`,
          minWidth: 120 * medicosColumna.length + 64,
          border: '1px solid var(--line)', borderRadius: 10, overflow: 'hidden',
        }}>
          <div style={{ background: 'var(--bg-soft)', borderBottom: '1px solid var(--line)' }} />
          {medicosColumna.map((m) => (
            <div key={m.id} style={{
              padding: '10px 6px', textAlign: 'center', fontSize: 12.5, fontWeight: 600,
              background: feriadoDelDia ? 'var(--red-tint)' : 'var(--bg-soft)',
              borderBottom: '1px solid var(--line)', borderLeft: '1px solid var(--line)',
            }}>
              {m.nombreCompleto}
            </div>
          ))}

          {horarios.map((hora) => (
            <Fragment key={hora}>
              <div style={{ padding: '8px 6px', fontSize: 12, color: 'var(--muted)', borderTop: '1px solid var(--line)', textAlign: 'right' }}>
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
      <LeyendaAgenda />

      {seleccionado && <DetalleTurno turno={seleccionado} onCerrar={() => setSeleccionadoId(null)} />}
    </div>
  );
}

export default function AdminAgendaTab() {
  const dispatch = useDispatch();
  const { medicos, agenda, agendaCargando, feriados, error, mensajeExito } = useSelector((state) => state.admin);
  const especialidades = useSelector((state) => state.turnos.especialidades);
  const [fecha, setFecha] = useState(hoyISO());
  const [especialidadId, setEspecialidadId] = useState('');
  const [medicoId, setMedicoId] = useState('');
  const [reservandoId, setReservandoId] = useState(null);
  const [vista, setVista] = useState('lista');
  const [denso, setDenso] = useState(false);

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

      {resumenDia && !agendaCargando && (
        <div className="card" style={{ marginBottom: 20, display: 'flex', gap: 24, flexWrap: 'wrap', alignItems: 'flex-start' }}>
          <div>
            <div style={{ fontSize: 13, color: 'var(--muted)' }}>{resumenDia.esHoy ? 'Hoy' : fecha}</div>
            <div style={{ fontSize: 22, fontWeight: 700, marginTop: 2 }}>
              {resumenDia.reservadosTotal} reservado{resumenDia.reservadosTotal === 1 ? '' : 's'}
            </div>
            <div style={{ fontSize: 13, color: 'var(--muted)' }}>{resumenDia.disponibles} disponible{resumenDia.disponibles === 1 ? '' : 's'}</div>
          </div>
          {resumenDia.proximos.length > 0 && (
            <div style={{ flex: 1, minWidth: 220 }}>
              <div style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 6 }}>
                {resumenDia.esHoy ? 'Próximos turnos' : 'Turnos del día'}
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                {resumenDia.proximos.map((t) => (
                  <div key={t.id} style={{ fontSize: 13.5 }}>
                    <strong>{t.horaInicio.slice(0, 5)}</strong> · {t.pacienteNombre} — {t.medicoNombre}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap', alignItems: 'center' }}>
        <button className={`btn ${vista === 'lista' ? 'btn-primary' : 'btn-outline'}`} style={{ padding: '7px 16px', fontSize: 13.5 }} onClick={() => setVista('lista')}>Lista</button>
        <button className={`btn ${vista === 'dia' ? 'btn-primary' : 'btn-outline'}`} style={{ padding: '7px 16px', fontSize: 13.5 }} onClick={() => setVista('dia')}>Día</button>
        <button className={`btn ${vista === 'semana' ? 'btn-primary' : 'btn-outline'}`} style={{ padding: '7px 16px', fontSize: 13.5 }} onClick={() => setVista('semana')}>Semana</button>
        {(vista === 'dia' || vista === 'semana') && (
          <button
            type="button"
            className={`btn ${denso ? 'btn-primary' : 'btn-outline'}`}
            style={{ padding: '7px 16px', fontSize: 13.5 }}
            onClick={() => setDenso((d) => !d)}
            title="Filas más chicas para ver más turnos sin scrollear"
          >
            Vista compacta
          </button>
        )}
      </div>
      <p style={{ fontSize: 12, color: 'var(--muted)', marginBottom: 16 }}>← → para cambiar de {vista === 'semana' ? 'semana' : 'día'} · T para volver a hoy</p>

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
        <div className="alert" style={{ background: 'var(--red-tint)', color: 'var(--red)', border: '1px solid var(--red)' }}>
          Feriado: {feriadoDelDia.nombre}
        </div>
      )}

      {vista === 'lista' && (
        <VistaLista agenda={agenda} agendaCargando={agendaCargando} medicoId={medicoId} reservandoId={reservandoId} setReservandoId={setReservandoId} />
      )}
      {vista === 'dia' && (
        <VistaDia agenda={agenda} agendaCargando={agendaCargando} feriados={feriados} medicos={medicos} medicoId={medicoId} fecha={fecha} denso={denso} />
      )}
      {vista === 'semana' && (
        <VistaSemana agenda={agenda} agendaCargando={agendaCargando} feriados={feriados} medicoId={medicoId} medicos={medicos} fecha={fecha} denso={denso} />
      )}
    </div>
  );
}
