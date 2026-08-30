import { Fragment, useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAgenda, fetchFeriados, fetchMedicosAdmin, reservarParaPaciente } from './adminSlice';
import { fetchEspecialidades } from '../turnos/turnosSlice';
import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../../constants/obrasSociales';

function hoyISO() {
  return new Date().toISOString().slice(0, 10);
}

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
          <label>DNI</label>
          <input required value={datos.dni} onChange={(e) => setDatos({ ...datos, dni: e.target.value })} placeholder="30123456" />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '2 1 220px' }}>
          <label>Nombre completo</label>
          <input required value={datos.nombreCompleto} onChange={(e) => setDatos({ ...datos, nombreCompleto: e.target.value })} />
        </div>
      </div>
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
          <label>Teléfono</label>
          <input required value={datos.telefono} onChange={(e) => setDatos({ ...datos, telefono: e.target.value })} placeholder="11 5950-5888" />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
          <label>Obra social</label>
          <select required value={datos.obraSocial} onChange={(e) => setDatos({ ...datos, obraSocial: e.target.value })}>
            <option value="">Seleccioná una obra social</option>
            <option value={OBRA_SOCIAL_PARTICULAR}>{OBRA_SOCIAL_PARTICULAR}</option>
            {OBRAS_SOCIALES.map((os) => <option key={os} value={os}>{os}</option>)}
            <option value={OBRA_SOCIAL_OTRA}>{OBRA_SOCIAL_OTRA}</option>
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 200px' }}>
          <label>Email (opcional)</label>
          <input type="email" value={datos.email} onChange={(e) => setDatos({ ...datos, email: e.target.value })} />
        </div>
      </div>
      <div className="field" style={{ marginBottom: 0 }}>
        <label>Motivo de consulta (opcional)</label>
        <input value={datos.motivoConsulta} onChange={(e) => setDatos({ ...datos, motivoConsulta: e.target.value })} />
      </div>
      <p style={{ fontSize: 12.5, color: 'var(--muted)' }}>Si el DNI ya está registrado, se usa esa cuenta y se ignoran los demás datos.</p>
      <div style={{ display: 'flex', gap: 8 }}>
        <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Reservando…' : 'Confirmar reserva'}</button>
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
          </div>
        </div>
        <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={onCerrar}>Cerrar</button>
      </div>
    </div>
  );
}

function VistaLista({ agenda, agendaCargando, medicoId, reservandoId, setReservandoId }) {
  return (
    <>
      {agendaCargando && <p style={{ color: 'var(--muted)' }}>Cargando agenda…</p>}
      {!agendaCargando && agenda.length === 0 && (
        <p style={{ color: 'var(--muted)' }}>No hay turnos generados para ese día{medicoId ? ' con ese médico' : ''}.</p>
      )}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {agenda.map((t) => (
          <div key={t.id} className="card" style={{ padding: '16px 20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ fontWeight: 600, fontSize: 15 }}>
                  {t.horaInicio.slice(0, 5)} a {t.horaFin.slice(0, 5)} · {t.medicoNombre}
                </div>
                <div style={{ fontSize: 13, color: 'var(--muted)' }}>{t.especialidadNombre}</div>
                {t.estado === 'RESERVADO' && (
                  <div style={{ marginTop: 8, fontSize: 14 }}>
                    <div><strong>{t.pacienteNombre}</strong> · DNI {t.pacienteDni}</div>
                    <div style={{ color: 'var(--muted)' }}>{t.pacienteTelefono} · {t.pacienteObraSocial}</div>
                    {t.motivoConsulta && <div style={{ color: 'var(--muted)', marginTop: 2 }}>Motivo: {t.motivoConsulta}</div>}
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
              </div>
            </div>
            {reservandoId === t.id && <FormularioReserva turno={t} onCancelar={() => setReservandoId(null)} />}
          </div>
        ))}
      </div>
    </>
  );
}

function VistaSemana({ agenda, agendaCargando, feriados, medicoId, medicos, fecha }) {
  const [seleccionado, setSeleccionado] = useState(null);

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
    return <p style={{ color: 'var(--muted)' }}>Elegí un médico arriba para ver la vista semanal en detalle.</p>;
  }

  if (!agendaCargando && horarios.length === 0) {
    return <p style={{ color: 'var(--muted)' }}>No hay turnos generados para {medicos.find((m) => String(m.id) === String(medicoId))?.nombreCompleto} en esta semana.</p>;
  }

  return (
    <div>
      <div style={{ overflowX: 'auto' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '64px repeat(7, minmax(96px, 1fr))', minWidth: 760, border: '1px solid var(--line)', borderRadius: 10, overflow: 'hidden' }}>
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
              {dias.map((dia) => {
                const feriado = feriadosPorDia[dia];
                const turno = porDiaYHora[`${dia}_${hora}`];
                if (feriado) {
                  return <div key={`${dia}-${hora}`} style={{ borderTop: '1px solid var(--line)', borderLeft: '1px solid var(--line)', background: 'repeating-linear-gradient(45deg, var(--bg-soft), var(--bg-soft) 6px, var(--line) 6px, var(--line) 7px)' }} />;
                }
                if (!turno) {
                  return <div key={`${dia}-${hora}`} style={{ borderTop: '1px solid var(--line)', borderLeft: '1px solid var(--line)' }} />;
                }
                const disponible = turno.estado === 'DISPONIBLE';
                return (
                  <button
                    key={`${dia}-${hora}`}
                    onClick={() => setSeleccionado(turno)}
                    style={{
                      borderTop: '1px solid var(--line)', borderLeft: '1px solid var(--line)', border: 'none',
                      borderTopColor: 'var(--line)', minHeight: 40, cursor: 'pointer',
                      background: disponible ? 'var(--teal-tint)' : 'var(--teal)',
                      color: disponible ? 'var(--teal-dark)' : '#fff',
                      fontSize: 11.5, fontWeight: 600, padding: '4px',
                    }}
                    title={disponible ? 'Disponible' : `${turno.pacienteNombre} (${turno.pacienteDni})`}
                  >
                    {disponible ? 'Libre' : (turno.pacienteNombre || '').split(' ')[0]}
                  </button>
                );
              })}
            </Fragment>
          ))}
        </div>
      </div>

      {seleccionado && <DetalleTurno turno={seleccionado} onCerrar={() => setSeleccionado(null)} />}
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

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      {mensajeExito && <div className="alert alert-success">{mensajeExito}</div>}

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        <button className={`btn ${vista === 'lista' ? 'btn-primary' : 'btn-outline'}`} style={{ padding: '7px 16px', fontSize: 13.5 }} onClick={() => setVista('lista')}>Lista</button>
        <button className={`btn ${vista === 'semana' ? 'btn-primary' : 'btn-outline'}`} style={{ padding: '7px 16px', fontSize: 13.5 }} onClick={() => setVista('semana')}>Semana</button>
      </div>

      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 20 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>{vista === 'semana' ? 'Semana de' : 'Día'}</label>
          <input type="date" value={fecha} onChange={(e) => setFecha(e.target.value)} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Especialidad</label>
          <select value={especialidadId} onChange={(e) => handleEspecialidadChange(e.target.value)}>
            <option value="">Todas las especialidades</option>
            {especialidades.map((esp) => <option key={esp.id} value={esp.id}>{esp.nombre}</option>)}
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Médico</label>
          <select value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
            <option value="">Todos los médicos</option>
            {medicosFiltrados.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
          </select>
        </div>
      </div>

      {vista === 'lista' && feriadoDelDia && (
        <div className="alert" style={{ background: 'var(--red-tint)', color: 'var(--red)', border: '1px solid var(--red)' }}>
          Feriado: {feriadoDelDia.nombre}
        </div>
      )}

      {vista === 'lista' ? (
        <VistaLista agenda={agenda} agendaCargando={agendaCargando} medicoId={medicoId} reservandoId={reservandoId} setReservandoId={setReservandoId} />
      ) : (
        <VistaSemana agenda={agenda} agendaCargando={agendaCargando} feriados={feriados} medicoId={medicoId} medicos={medicos} fecha={fecha} />
      )}
    </div>
  );
}
