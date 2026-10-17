import { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { FileUp } from 'lucide-react';
import {
  fetchMisPlanesKinesiologia, fetchHorariosDisponiblesKinesiologia, crearPlanKinesiologia, cancelarPlanKinesiologia,
  pausarPlanKinesiologia, reanudarPlanKinesiologia, limpiarMensajesKinesiologia, limpiarHorariosDisponibles,
} from './kinesiologiaSlice';
import { fetchEspecialidades, fetchMedicos } from '../turnos/turnosSlice';
import { hoyISO } from '../../utils/formato';
import { useConfirm } from '../../components/ui/useConfirm';
import Goniometro from '../../components/ui/Goniometro';

const DIA_LABEL = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};
const ORDEN_DIAS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

function claveHorario(h) {
  return `${h.medicoId}-${h.diaSemana}-${h.horaInicio}`;
}

const ESTADO_LABEL = {
  PENDIENTE_VALIDACION: 'En revisión',
  VALIDADO: 'Activo',
  RECHAZADO: 'Rechazado',
  PAUSADO: 'Pausado',
  CANCELADO: 'Cancelado',
};

const BADGE_ESTADO = {
  PENDIENTE_VALIDACION: 'badge-pendiente',
  VALIDADO: 'badge-confirmado',
  RECHAZADO: 'badge-ausente',
  PAUSADO: 'badge-cancelado',
  CANCELADO: 'badge-cancelado',
};

function aFecha(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}
const fmtSesion = new Intl.DateTimeFormat('es-AR', { weekday: 'short', day: 'numeric', month: 'short' });
function fechaSesion(iso) {
  return fmtSesion.format(aFecha(iso)).replace(/\./g, '');
}

function NuevoPlanForm({ medicos }) {
  const dispatch = useDispatch();
  const horariosDisponibles = useSelector((state) => state.kinesiologia.horariosDisponibles);
  const cargandoHorarios = useSelector((state) => state.kinesiologia.cargandoHorarios);

  const [tieneOrden, setTieneOrden] = useState(null);
  const [modoIndiferente, setModoIndiferente] = useState(false);
  const [medicoId, setMedicoId] = useState('');
  const [cantidadSesiones, setCantidadSesiones] = useState(10);
  const [fechaInicio, setFechaInicio] = useState(hoyISO());
  const [seleccionados, setSeleccionados] = useState([]);
  const [ordenMedica, setOrdenMedica] = useState(null);
  const [enviando, setEnviando] = useState(false);

  useEffect(() => {
    if (tieneOrden !== true) return;
    if (modoIndiferente) {
      dispatch(fetchHorariosDisponiblesKinesiologia());
    } else if (medicoId) {
      dispatch(fetchHorariosDisponiblesKinesiologia(medicoId));
    } else {
      dispatch(limpiarHorariosDisponibles());
    }
    setSeleccionados([]);
  }, [tieneOrden, modoIndiferente, medicoId, dispatch]);

  // Agrupados por día: cada día es un renglón y sus horarios, celdas que se marcan.
  const horariosPorDia = useMemo(() => {
    const ordenados = [...horariosDisponibles].sort((a, b) => {
      const diffDia = ORDEN_DIAS.indexOf(a.diaSemana) - ORDEN_DIAS.indexOf(b.diaSemana);
      if (diffDia !== 0) return diffDia;
      if (a.horaInicio !== b.horaInicio) return a.horaInicio.localeCompare(b.horaInicio);
      return a.medicoNombre.localeCompare(b.medicoNombre);
    });
    const grupos = new Map();
    for (const h of ordenados) {
      if (!grupos.has(h.diaSemana)) grupos.set(h.diaSemana, []);
      grupos.get(h.diaSemana).push(h);
    }
    return [...grupos.entries()];
  }, [horariosDisponibles]);

  const medicoElegido = seleccionados[0]?.medicoId;

  function toggleHorario(h) {
    const clave = claveHorario(h);
    setSeleccionados((prev) => prev.some((s) => claveHorario(s) === clave)
      ? prev.filter((s) => claveHorario(s) !== clave)
      : [...prev, h]);
  }

  function handleSubmit(e) {
    e.preventDefault();
    if (!ordenMedica || seleccionados.length === 0) return;
    const formData = new FormData();
    formData.append('medicoId', modoIndiferente ? medicoElegido : medicoId);
    formData.append('cantidadSesiones', cantidadSesiones);
    formData.append('fechaInicio', fechaInicio);
    seleccionados.forEach((h) => {
      formData.append('diasSemana', h.diaSemana);
      formData.append('horasInicio', h.horaInicio);
    });
    formData.append('ordenMedica', ordenMedica);
    setEnviando(true);
    dispatch(crearPlanKinesiologia(formData)).unwrap()
      .then(() => {
        setTieneOrden(null);
        setModoIndiferente(false);
        setMedicoId('');
        setCantidadSesiones(10);
        setSeleccionados([]);
        setOrdenMedica(null);
        dispatch(limpiarHorariosDisponibles());
      })
      .finally(() => setEnviando(false));
  }

  if (tieneOrden === null) {
    return (
      <div className="card kine-pregunta">
        <h2>¿Tenés orden médica para kinesiología?</h2>
        <p>Para atenderte en kinesiología hace falta una orden médica vigente. Después la vas a subir como foto o PDF.</p>
        <div className="kine-pregunta-opciones">
          <button type="button" className="btn btn-primary btn-lg" onClick={() => setTieneOrden(true)}>Sí, tengo orden</button>
          <button type="button" className="btn btn-outline btn-lg" onClick={() => setTieneOrden(false)}>Todavía no</button>
        </div>
      </div>
    );
  }

  if (tieneOrden === false) {
    return (
      <div className="card kine-pregunta">
        <h2>Primero conseguí la orden</h2>
        <p>Sin orden médica no podemos armar el plan. Pedísela a tu médico de cabecera y volvé cuando la tengas: el plan se arma en un par de minutos.</p>
        <div className="kine-pregunta-opciones">
          <button type="button" className="btn btn-outline" onClick={() => setTieneOrden(null)}>Volver</button>
        </div>
      </div>
    );
  }

  const puedeVerHorarios = modoIndiferente || medicoId;

  return (
    <form onSubmit={handleSubmit} className="card kine-form">
      <h2>Armá tu plan de sesiones</h2>

      <fieldset className="kine-bloque">
        <legend>¿Con quién te querés atender?</legend>
        <div className="kine-modos">
          <label className={`kine-modo${!modoIndiferente ? ' activo' : ''}`}>
            <input type="radio" name="modoMedico" checked={!modoIndiferente}
              onChange={() => { setModoIndiferente(false); setMedicoId(''); }} />
            <span><strong>Con un kinesiólogo en particular</strong>Elegís a quién y ves sus horarios.</span>
          </label>
          <label className={`kine-modo${modoIndiferente ? ' activo' : ''}`}>
            <input type="radio" name="modoMedico" checked={modoIndiferente}
              onChange={() => { setModoIndiferente(true); setMedicoId(''); }} />
            <span><strong>Me da igual quién</strong>Ves todos los horarios libres del centro.</span>
          </label>
        </div>
        {!modoIndiferente && (
          <div className="field" style={{ marginTop: 14, marginBottom: 0 }}>
            <label htmlFor="kineMedico">Kinesiólogo</label>
            <select id="kineMedico" required value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
              <option value="">Elegí un kinesiólogo</option>
              {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
            </select>
          </div>
        )}
      </fieldset>

      <div className="campos-2">
        <div className="field">
          <label htmlFor="kineCantidadSesiones">Sesiones indicadas en la orden</label>
          <input id="kineCantidadSesiones" type="number" inputMode="numeric" min={1} max={60} required value={cantidadSesiones} onChange={(e) => setCantidadSesiones(e.target.value)} />
        </div>
        <div className="field">
          <label htmlFor="kineFechaInicio">Empezar desde</label>
          <input id="kineFechaInicio" type="date" min={hoyISO()} required value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} />
        </div>
      </div>

      <fieldset className="kine-bloque">
        <legend>Horarios fijos de cada semana</legend>
        <p className="field-hint" style={{ marginBottom: 12 }}>Marcá uno o más: se repiten todas las semanas hasta completar las sesiones.</p>
        {!puedeVerHorarios && (
          <p className="kine-vacio">Elegí primero un kinesiólogo para ver sus horarios.</p>
        )}
        {puedeVerHorarios && cargandoHorarios && (
          <div className="kine-dias" aria-hidden="true">
            {[0, 1, 2].map((i) => <span key={i} className="skeleton" style={{ height: 52 }} />)}
          </div>
        )}
        {puedeVerHorarios && !cargandoHorarios && horariosPorDia.length === 0 && (
          <p className="kine-vacio">No hay horarios libres por ahora. Llamanos y lo resolvemos por teléfono.</p>
        )}
        {puedeVerHorarios && !cargandoHorarios && horariosPorDia.length > 0 && (
          <div className="kine-dias">
            {horariosPorDia.map(([dia, horarios]) => (
              <div key={dia} className="kine-dia" role="group" aria-label={DIA_LABEL[dia]}>
                <span className="kine-dia-nombre">{DIA_LABEL[dia]}</span>
                <div className="kine-dia-horarios">
                  {horarios.map((h) => {
                    const clave = claveHorario(h);
                    const marcado = seleccionados.some((s) => claveHorario(s) === clave);
                    const deshabilitado = modoIndiferente && medicoElegido && h.medicoId !== medicoElegido;
                    return (
                      <button
                        type="button"
                        key={clave}
                        className="horario kine-horario"
                        aria-pressed={marcado}
                        disabled={deshabilitado}
                        onClick={() => toggleHorario(h)}
                      >
                        {h.horaInicio.slice(0, 5)}
                        {modoIndiferente && <span>{h.medicoNombre}</span>}
                      </button>
                    );
                  })}
                </div>
              </div>
            ))}
          </div>
        )}
        {modoIndiferente && seleccionados.length > 0 && (
          <p className="field-hint" style={{ marginTop: 10 }}>
            Elegiste horarios con {medicos.find((m) => m.id === medicoElegido)?.nombreCompleto || seleccionados[0].medicoNombre}. Para cambiar de kinesiólogo, desmarcá todos los horarios primero.
          </p>
        )}
      </fieldset>

      <div className="field">
        <label htmlFor="kineOrdenMedica">Orden médica</label>
        <label htmlFor="kineOrdenMedica" className={`archivo${ordenMedica ? ' cargado' : ''}`}>
          <FileUp size={22} aria-hidden="true" />
          <span>{ordenMedica ? ordenMedica.name : 'Subí una foto o PDF de la orden'}</span>
        </label>
        <input id="kineOrdenMedica" className="archivo-input" type="file" required accept="image/jpeg,image/png,application/pdf" onChange={(e) => setOrdenMedica(e.target.files[0] || null)} />
      </div>

      <button type="submit" className="btn btn-primary btn-lg btn-block" disabled={enviando || seleccionados.length === 0}>
        {enviando && <span className="spinner" aria-hidden="true" />}
        {enviando ? 'Enviando…' : seleccionados.length > 0 ? `Enviar plan (${seleccionados.length} ${seleccionados.length === 1 ? 'horario' : 'horarios'} por semana)` : 'Marcá al menos un horario'}
      </button>
      <p className="field-hint" style={{ marginTop: 10, textAlign: 'center' }}>
        El centro revisa la orden antes de confirmar las sesiones.
      </p>
    </form>
  );
}

function PlanCard({ plan }) {
  const dispatch = useDispatch();
  const confirmar = useConfirm();
  const hoy = hoyISO();

  async function handlePausar() {
    const ok = await confirmar({
      titulo: '¿Pausar el plan?',
      detalle: 'Se liberan las sesiones futuras ya generadas. Podés reanudarlo después.',
      confirmarTexto: 'Pausar',
    });
    if (ok) dispatch(pausarPlanKinesiologia(plan.id));
  }

  async function handleCancelar() {
    const ok = await confirmar({
      titulo: '¿Cancelar el plan?',
      detalle: 'Se liberan las sesiones futuras y no se puede deshacer.',
      confirmarTexto: 'Sí, cancelar plan',
      peligroso: true,
    });
    if (ok) dispatch(cancelarPlanKinesiologia(plan.id));
  }

  // Lo que se puede afirmar con los datos que hay: cuántas sesiones reservadas ya quedaron
  // atrás en el calendario (no si el paciente asistió — eso no viene en el plan).
  const reservadas = plan.sesiones.filter((s) => s.estado === 'RESERVADO');
  const pasadas = reservadas.filter((s) => s.fecha < hoy).length;
  const proxima = reservadas.find((s) => s.fecha >= hoy);
  const total = plan.cantidadSesiones || 1;

  return (
    <article className="plan">
      <div className="plan-medidor">
        <Goniometro progreso={pasadas / total} compacto />
        <p className="plan-medida"><strong>{pasadas}</strong> de {plan.cantidadSesiones}</p>
        <p className="plan-medida-texto">sesiones ya pasaron</p>
      </div>
      <div className="plan-cuerpo">
        <div className="plan-cabecera">
          <h3>{plan.medicoNombre}</h3>
          <span className={`badge ${BADGE_ESTADO[plan.estado] || 'badge-cancelado'}`}>{ESTADO_LABEL[plan.estado] || plan.estado}</span>
        </div>
        <p className="plan-dato">
          Desde el {fechaSesion(plan.fechaInicio)} · {plan.sesionesGeneradas} de {plan.cantidadSesiones} sesiones agendadas
        </p>
        {proxima && (
          <p className="plan-proxima">Próxima sesión: <strong>{fechaSesion(proxima.fecha)} · {proxima.horaInicio.slice(0, 5)}</strong></p>
        )}

        {plan.estado === 'RECHAZADO' && plan.motivoRechazo && (
          <div className="alert alert-error" role="alert" style={{ marginTop: 12 }}>Motivo: {plan.motivoRechazo}</div>
        )}

        {plan.sesiones.length > 0 && (
          <details className="plan-sesiones">
            <summary>Ver las {plan.sesiones.length} sesiones</summary>
            <ul>
              {plan.sesiones.map((s) => (
                <li key={s.turnoId} className={`badge ${s.estado === 'RESERVADO' ? (s.fecha < hoy ? 'badge-llego' : 'badge-confirmado') : s.estado === 'DISPONIBLE' ? 'badge-disponible' : 'badge-cancelado'}`}>
                  {fechaSesion(s.fecha)} {s.horaInicio.slice(0, 5)}
                </li>
              ))}
            </ul>
          </details>
        )}

        {(plan.estado === 'VALIDADO' || plan.estado === 'PENDIENTE_VALIDACION') && (
          <div className="plan-acciones">
            <button className="btn btn-outline" onClick={handlePausar}>Pausar</button>
            <button className="btn btn-ghost btn-peligro" onClick={handleCancelar}>Cancelar plan</button>
          </div>
        )}

        {plan.estado === 'PAUSADO' && (
          <div className="plan-acciones">
            <button className="btn btn-primary" onClick={() => dispatch(reanudarPlanKinesiologia({ id: plan.id }))}>
              Reanudar desde hoy
            </button>
          </div>
        )}
      </div>
    </article>
  );
}

export default function KinesiologiaPage() {
  const dispatch = useDispatch();
  const { misPlanes, error, mensajeExito } = useSelector((state) => state.kinesiologia);
  const especialidades = useSelector((state) => state.turnos.especialidades);
  const medicosPorEspecialidad = useSelector((state) => state.turnos.medicos);

  const especialidadKinesiologia = useMemo(() => especialidades.find((e) => e.esKinesiologia), [especialidades]);

  useEffect(() => {
    dispatch(fetchEspecialidades());
    dispatch(fetchMisPlanesKinesiologia());
    return () => dispatch(limpiarMensajesKinesiologia());
  }, [dispatch]);

  useEffect(() => {
    if (especialidadKinesiologia) {
      dispatch(fetchMedicos(especialidadKinesiologia.id));
    }
  }, [especialidadKinesiologia, dispatch]);

  return (
    <div className="kine">
      <div className="page-head">
        <h1>Kinesiología</h1>
        <p>Tus sesiones se arman como un plan semanal, a partir de la orden de tu médico.</p>
      </div>

      {error && <div className="alert alert-error" role="alert" style={{ marginTop: 16 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginTop: 16 }}>{mensajeExito}</div>}

      {misPlanes.length > 0 && (
        <section className="mis-turnos-seccion" aria-labelledby="planesTitulo">
          <h2 id="planesTitulo" className="seccion-titulo">Tus planes</h2>
          <div className="stagger mis-turnos-lista">
            {misPlanes.map((p) => <PlanCard key={p.id} plan={p} />)}
          </div>
        </section>
      )}

      <section className="mis-turnos-seccion" aria-label="Nuevo plan">
        {especialidadKinesiologia && <NuevoPlanForm medicos={medicosPorEspecialidad} />}
      </section>
    </div>
  );
}
