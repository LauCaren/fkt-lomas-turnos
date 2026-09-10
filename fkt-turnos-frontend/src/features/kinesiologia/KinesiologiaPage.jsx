import { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchMisPlanesKinesiologia, fetchHorariosDisponiblesKinesiologia, crearPlanKinesiologia, cancelarPlanKinesiologia,
  pausarPlanKinesiologia, reanudarPlanKinesiologia, limpiarMensajesKinesiologia, limpiarHorariosDisponibles,
} from './kinesiologiaSlice';
import { fetchEspecialidades, fetchMedicos } from '../turnos/turnosSlice';
import { hoyISO } from '../../utils/formato';

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
  PENDIENTE_VALIDACION: 'Pendiente de revisión',
  VALIDADO: 'Activo',
  RECHAZADO: 'Rechazado',
  PAUSADO: 'Pausado',
  CANCELADO: 'Cancelado',
};

const BADGE_ESTADO = {
  PENDIENTE_VALIDACION: 'badge-pendiente',
  VALIDADO: 'badge-disponible',
  RECHAZADO: 'badge-reservado',
  PAUSADO: 'badge-cancelado',
  CANCELADO: 'badge-cancelado',
};

function NuevoPlanForm({ medicoKinesiologiaId, medicos }) {
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

  const horariosOrdenados = useMemo(() => (
    [...horariosDisponibles].sort((a, b) => {
      const diffDia = ORDEN_DIAS.indexOf(a.diaSemana) - ORDEN_DIAS.indexOf(b.diaSemana);
      if (diffDia !== 0) return diffDia;
      if (a.medicoNombre !== b.medicoNombre) return a.medicoNombre.localeCompare(b.medicoNombre);
      return a.horaInicio.localeCompare(b.horaInicio);
    })
  ), [horariosDisponibles]);

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
      <div className="card">
        <h3 style={{ fontSize: 17, marginBottom: 10 }}>¿Tenés orden médica para kinesiología?</h3>
        <p style={{ color: 'var(--muted)', marginBottom: 14, fontSize: 14 }}>
          Para atenderte en kinesiología es obligatorio tener una orden médica vigente.
        </p>
        <div style={{ display: 'flex', gap: 10 }}>
          <button className="btn btn-primary" onClick={() => setTieneOrden(true)}>Sí, tengo</button>
          <button className="btn btn-outline" onClick={() => setTieneOrden(false)}>No tengo</button>
        </div>
      </div>
    );
  }

  if (tieneOrden === false) {
    return (
      <div className="card">
        <div className="alert alert-error" role="alert">
          No podés reservar un turno de kinesiología sin orden médica. Conseguila con tu médico de cabecera y volvé
          a intentarlo.
        </div>
        <button className="btn btn-outline" style={{ marginTop: 12 }} onClick={() => setTieneOrden(null)}>Volver</button>
      </div>
    );
  }

  return (
    <div className="card">
      <h3 style={{ fontSize: 17, marginBottom: 14 }}>Nuevo plan de sesiones</h3>
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label>¿Con quién te querés atender?</label>
          <div style={{ display: 'flex', gap: 16, marginBottom: 4 }}>
            <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 400 }}>
              <input type="radio" name="modoMedico" checked={!modoIndiferente}
                onChange={() => { setModoIndiferente(false); setMedicoId(''); }} />
              Un kinesiólogo en particular
            </label>
            <label style={{ display: 'flex', alignItems: 'center', gap: 6, fontWeight: 400 }}>
              <input type="radio" name="modoMedico" checked={modoIndiferente}
                onChange={() => { setModoIndiferente(true); setMedicoId(''); }} />
              Me da igual quién me atienda
            </label>
          </div>
          {!modoIndiferente && (
            <select required value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
              <option value="">Seleccioná un kinesiólogo</option>
              {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
            </select>
          )}
        </div>
        <div className="field">
          <label htmlFor="kineCantidadSesiones">Cantidad de sesiones indicadas</label>
          <input id="kineCantidadSesiones" type="number" min={1} max={60} required value={cantidadSesiones} onChange={(e) => setCantidadSesiones(e.target.value)} style={{ width: 100 }} />
        </div>
        <div className="field">
          <label htmlFor="kineFechaInicio">Fecha de inicio</label>
          <input id="kineFechaInicio" type="date" min={hoyISO()} required value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} />
        </div>
        <div className="field" role="group" aria-labelledby="kineHorariosFijosLabel">
          <label id="kineHorariosFijosLabel">Horarios fijos semanales</label>
          {!modoIndiferente && !medicoId && (
            <p style={{ fontSize: 13, color: 'var(--muted)' }}>Elegí primero un kinesiólogo para ver sus horarios disponibles.</p>
          )}
          {(modoIndiferente || medicoId) && cargandoHorarios && (
            <div style={{ marginTop: 6 }}>
              <div className="skeleton skeleton-line" style={{ width: '65%' }} />
              <div className="skeleton skeleton-line" style={{ width: '50%' }} />
              <div className="skeleton skeleton-line" style={{ width: '58%' }} />
            </div>
          )}
          {(modoIndiferente || medicoId) && !cargandoHorarios && horariosOrdenados.length === 0 && (
            <p style={{ fontSize: 13, color: 'var(--muted)' }}>No hay horarios cargados por ahora.</p>
          )}
          {(modoIndiferente || medicoId) && !cargandoHorarios && horariosOrdenados.length > 0 && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 6, maxHeight: 260, overflowY: 'auto', border: '1px solid var(--border)', borderRadius: 8, padding: 10 }}>
              {horariosOrdenados.map((h) => {
                const clave = claveHorario(h);
                const marcado = seleccionados.some((s) => claveHorario(s) === clave);
                const deshabilitado = modoIndiferente && medicoElegido && h.medicoId !== medicoElegido;
                return (
                  <label key={clave} style={{ display: 'flex', alignItems: 'center', gap: 8, fontWeight: 400, opacity: deshabilitado ? 0.4 : 1 }}>
                    <input type="checkbox" checked={marcado} disabled={deshabilitado} onChange={() => toggleHorario(h)} />
                    {DIA_LABEL[h.diaSemana]} {h.horaInicio.slice(0, 5)}
                    {modoIndiferente && ` — ${h.medicoNombre}`}
                  </label>
                );
              })}
            </div>
          )}
          {modoIndiferente && seleccionados.length > 0 && (
            <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 6 }}>
              Elegiste horarios con {medicos.find((m) => m.id === medicoElegido)?.nombreCompleto || seleccionados[0].medicoNombre}. Para cambiar de kinesiólogo, deseleccioná todos los horarios primero.
            </p>
          )}
        </div>
        <div className="field">
          <label htmlFor="kineOrdenMedica">Orden médica (foto o PDF)</label>
          <input id="kineOrdenMedica" type="file" required accept="image/jpeg,image/png,application/pdf" onChange={(e) => setOrdenMedica(e.target.files[0] || null)} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={enviando || seleccionados.length === 0}>
          {enviando && <span className="spinner" aria-hidden="true" />}
          {enviando ? 'Enviando…' : 'Enviar plan'}
        </button>
        <p style={{ fontSize: 12.5, color: 'var(--muted)', marginTop: 8 }}>
          Un admin va a revisar la orden médica antes de habilitar los turnos.
        </p>
      </form>
    </div>
  );
}

function PlanCard({ plan }) {
  const dispatch = useDispatch();

  return (
    <div className="card" style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 10 }}>
        <div>
          <div style={{ fontWeight: 600 }}>{plan.medicoNombre}</div>
          <div style={{ fontSize: 13, color: 'var(--muted)' }}>
            {plan.sesionesGeneradas} de {plan.cantidadSesiones} sesiones generadas · desde {plan.fechaInicio}
          </div>
        </div>
        <span className={`badge ${BADGE_ESTADO[plan.estado] || 'badge-cancelado'}`}>{ESTADO_LABEL[plan.estado]}</span>
      </div>

      {plan.estado === 'RECHAZADO' && plan.motivoRechazo && (
        <div className="alert alert-error" role="alert" style={{ marginTop: 10 }}>Motivo: {plan.motivoRechazo}</div>
      )}

      {plan.sesiones.length > 0 && (
        <div style={{ marginTop: 10, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {plan.sesiones.map((s) => (
            <span key={s.turnoId} className={`badge badge-${s.estado === 'RESERVADO' ? 'reservado' : s.estado === 'DISPONIBLE' ? 'disponible' : 'cancelado'}`}>
              {s.fecha} {s.horaInicio.slice(0, 5)}
            </span>
          ))}
        </div>
      )}

      {(plan.estado === 'VALIDADO' || plan.estado === 'PENDIENTE_VALIDACION') && (
        <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
          <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }}
            onClick={() => window.confirm('¿Pausar el plan? Se liberan las sesiones futuras.') && dispatch(pausarPlanKinesiologia(plan.id))}>
            Pausar
          </button>
          <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }}
            onClick={() => window.confirm('¿Cancelar el plan? Se liberan las sesiones futuras y no se puede deshacer.') && dispatch(cancelarPlanKinesiologia(plan.id))}>
            Cancelar
          </button>
        </div>
      )}

      {plan.estado === 'PAUSADO' && (
        <div style={{ marginTop: 12 }}>
          <button className="btn btn-primary" style={{ padding: '6px 14px', fontSize: 13 }}
            onClick={() => dispatch(reanudarPlanKinesiologia({ id: plan.id }))}>
            Reanudar desde hoy
          </button>
        </div>
      )}
    </div>
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
    <div style={{ maxWidth: 640, margin: '0 auto' }}>
      <div className="page-head">
        <span className="eyebrow">Kinesiología</span>
        <h1>Plan de sesiones</h1>
      </div>

      {error && <div className="alert alert-error" role="alert" style={{ marginTop: 16 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginTop: 16 }}>{mensajeExito}</div>}

      <div style={{ marginTop: 20 }}>
        {especialidadKinesiologia && <NuevoPlanForm medicoKinesiologiaId={especialidadKinesiologia.id} medicos={medicosPorEspecialidad} />}
      </div>

      {misPlanes.length > 0 && (
        <>
          <h2 style={{ fontSize: 20, marginTop: 32, marginBottom: 16 }}>Mis planes</h2>
          <div className="stagger">
            {misPlanes.map((p) => <PlanCard key={p.id} plan={p} />)}
          </div>
        </>
      )}
    </div>
  );
}
