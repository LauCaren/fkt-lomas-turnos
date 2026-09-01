import { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchMisPlanesKinesiologia, crearPlanKinesiologia, cancelarPlanKinesiologia,
  pausarPlanKinesiologia, reanudarPlanKinesiologia, limpiarMensajesKinesiologia,
} from './kinesiologiaSlice';
import { fetchEspecialidades, fetchMedicos } from '../turnos/turnosSlice';

const DIAS = [
  { value: 'MONDAY', label: 'Lunes' },
  { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' },
  { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' },
];

const ESTADO_LABEL = {
  PENDIENTE_VALIDACION: 'Pendiente de revisión',
  VALIDADO: 'Activo',
  RECHAZADO: 'Rechazado',
  PAUSADO: 'Pausado',
  CANCELADO: 'Cancelado',
};

function hoyISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function NuevoPlanForm({ medicoKinesiologiaId, medicos }) {
  const dispatch = useDispatch();
  const [tieneOrden, setTieneOrden] = useState(null);
  const [medicoId, setMedicoId] = useState('');
  const [cantidadSesiones, setCantidadSesiones] = useState(10);
  const [fechaInicio, setFechaInicio] = useState(hoyISO());
  const [horarios, setHorarios] = useState([{ diaSemana: 'MONDAY', horaInicio: '15:00' }]);
  const [ordenMedica, setOrdenMedica] = useState(null);
  const [enviando, setEnviando] = useState(false);

  function agregarHorario() {
    setHorarios([...horarios, { diaSemana: 'MONDAY', horaInicio: '15:00' }]);
  }

  function quitarHorario(i) {
    setHorarios(horarios.filter((_, idx) => idx !== i));
  }

  function actualizarHorario(i, campo, valor) {
    setHorarios(horarios.map((h, idx) => idx === i ? { ...h, [campo]: valor } : h));
  }

  function handleSubmit(e) {
    e.preventDefault();
    if (!ordenMedica) return;
    const formData = new FormData();
    formData.append('medicoId', medicoId);
    formData.append('cantidadSesiones', cantidadSesiones);
    formData.append('fechaInicio', fechaInicio);
    horarios.forEach((h) => {
      formData.append('diasSemana', h.diaSemana);
      formData.append('horasInicio', h.horaInicio);
    });
    formData.append('ordenMedica', ordenMedica);
    setEnviando(true);
    dispatch(crearPlanKinesiologia(formData)).unwrap()
      .then(() => {
        setTieneOrden(null);
        setMedicoId('');
        setCantidadSesiones(10);
        setHorarios([{ diaSemana: 'MONDAY', horaInicio: '15:00' }]);
        setOrdenMedica(null);
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
        <div className="alert alert-error">
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
          <label>Kinesiólogo</label>
          <select required value={medicoId} onChange={(e) => setMedicoId(e.target.value)}>
            <option value="">Seleccioná un kinesiólogo</option>
            {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
          </select>
        </div>
        <div className="field">
          <label>Cantidad de sesiones indicadas</label>
          <input type="number" min={1} max={60} required value={cantidadSesiones} onChange={(e) => setCantidadSesiones(e.target.value)} style={{ width: 100 }} />
        </div>
        <div className="field">
          <label>Fecha de inicio</label>
          <input type="date" min={hoyISO()} required value={fechaInicio} onChange={(e) => setFechaInicio(e.target.value)} />
        </div>
        <div className="field">
          <label>Horarios fijos semanales</label>
          {horarios.map((h, i) => (
            <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 8, alignItems: 'center' }}>
              <select value={h.diaSemana} onChange={(e) => actualizarHorario(i, 'diaSemana', e.target.value)}>
                {DIAS.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
              </select>
              <input type="time" value={h.horaInicio} onChange={(e) => actualizarHorario(i, 'horaInicio', e.target.value)} />
              {horarios.length > 1 && (
                <button type="button" className="btn btn-outline" style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => quitarHorario(i)}>Quitar</button>
              )}
            </div>
          ))}
          <button type="button" className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={agregarHorario}>
            + Agregar otro horario
          </button>
        </div>
        <div className="field">
          <label>Orden médica (foto o PDF)</label>
          <input type="file" required accept="image/jpeg,image/png,application/pdf" onChange={(e) => setOrdenMedica(e.target.files[0] || null)} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={enviando}>{enviando ? 'Enviando…' : 'Enviar plan'}</button>
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
        <span className="badge">{ESTADO_LABEL[plan.estado]}</span>
      </div>

      {plan.estado === 'RECHAZADO' && plan.motivoRechazo && (
        <div className="alert alert-error" style={{ marginTop: 10 }}>Motivo: {plan.motivoRechazo}</div>
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
      <span className="eyebrow">Kinesiología</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Plan de sesiones</h1>

      {error && <div className="alert alert-error" style={{ marginTop: 16 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" style={{ marginTop: 16 }}>{mensajeExito}</div>}

      <div style={{ marginTop: 20 }}>
        {especialidadKinesiologia && <NuevoPlanForm medicoKinesiologiaId={especialidadKinesiologia.id} medicos={medicosPorEspecialidad} />}
      </div>

      {misPlanes.length > 0 && (
        <>
          <h2 style={{ fontSize: 20, marginTop: 32, marginBottom: 16 }}>Mis planes</h2>
          {misPlanes.map((p) => <PlanCard key={p.id} plan={p} />)}
        </>
      )}
    </div>
  );
}
