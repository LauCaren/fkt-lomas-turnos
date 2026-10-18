import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchPlanesKinesiologiaAdmin, crearPlanKinesiologiaPorTelefono, validarPlanKinesiologia,
  rechazarPlanKinesiologia, cancelarPlanKinesiologiaAdmin, limpiarMensajesKinesiologia,
} from './kinesiologiaSlice';
import { fetchMedicosAdmin } from '../admin/adminSlice';
import SelectorObraSocial from '../../components/SelectorObraSocial';
import { verArchivo } from '../../api/descargas';
import { hoyISO } from '../../utils/formato';
import EstadoVacio from '../../components/EstadoVacio';
import { useConfirm } from '../../components/ui/useConfirm';

function verOrdenMedica(planId) {
  return verArchivo(`/api/admin/kinesiologia/planes/${planId}/orden-medica`);
}

const DIAS = [
  { value: 'MONDAY', label: 'Lunes' },
  { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' },
  { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' },
];

const ESTADOS = [
  { value: '', label: 'Todos' },
  { value: 'PENDIENTE_VALIDACION', label: 'Pendientes de revisión' },
  { value: 'VALIDADO', label: 'Activos' },
  { value: 'RECHAZADO', label: 'Rechazados' },
  { value: 'PAUSADO', label: 'Pausados' },
  { value: 'CANCELADO', label: 'Cancelados' },
];

const TELEFONICO_VACIO = {
  dni: '', nombreCompleto: '', telefono: '', obraSocial: '', email: '',
  medicoId: '', cantidadSesiones: 10, fechaInicio: hoyISO(),
};

function NuevoPlanTelefonico({ medicosKinesiologia }) {
  const dispatch = useDispatch();
  const [form, setForm] = useState(TELEFONICO_VACIO);
  const [horarios, setHorarios] = useState([{ id: crypto.randomUUID(), diaSemana: 'MONDAY', horaInicio: '15:00' }]);
  const [ordenMedica, setOrdenMedica] = useState(null);
  const [abierto, setAbierto] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    if (!ordenMedica) return;
    const formData = new FormData();
    Object.entries(form).forEach(([k, v]) => formData.append(k, v));
    horarios.forEach((h) => {
      formData.append('diasSemana', h.diaSemana);
      formData.append('horasInicio', h.horaInicio);
    });
    formData.append('ordenMedica', ordenMedica);
    dispatch(crearPlanKinesiologiaPorTelefono(formData)).unwrap().then(() => {
      setForm(TELEFONICO_VACIO);
      setHorarios([{ id: crypto.randomUUID(), diaSemana: 'MONDAY', horaInicio: '15:00' }]);
      setOrdenMedica(null);
      setAbierto(false);
    }).catch(() => {});
  }

  if (!abierto) {
    return <button className="btn btn-outline" onClick={() => setAbierto(true)}>Cargar plan por teléfono</button>;
  }

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <h3 style={{ fontSize: 16, marginBottom: 12 }}>Cargar plan en nombre de un paciente</h3>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
            <label htmlFor="kineTelefonicoDni">DNI</label>
            <input id="kineTelefonicoDni" required value={form.dni} onChange={(e) => setForm({ ...form, dni: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '2 1 220px' }}>
            <label htmlFor="kineTelefonicoNombre">Nombre completo</label>
            <input id="kineTelefonicoNombre" required value={form.nombreCompleto} onChange={(e) => setForm({ ...form, nombreCompleto: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
            <label htmlFor="kineTelefonicoTelefono">Teléfono</label>
            <input id="kineTelefonicoTelefono" required value={form.telefono} onChange={(e) => setForm({ ...form, telefono: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
            <label htmlFor="kineTelefonicoObraSocial">Obra social</label>
            <SelectorObraSocial id="kineTelefonicoObraSocial" value={form.obraSocial} onChange={(e) => setForm({ ...form, obraSocial: e.target.value })} placeholder="Seleccioná" />
          </div>
        </div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 200px' }}>
            <label htmlFor="kineTelefonicoMedico">Kinesiólogo</label>
            <select id="kineTelefonicoMedico" required value={form.medicoId} onChange={(e) => setForm({ ...form, medicoId: e.target.value })}>
              <option value="">Seleccioná</option>
              {medicosKinesiologia.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 120px' }}>
            <label htmlFor="kineTelefonicoSesiones">Sesiones</label>
            <input id="kineTelefonicoSesiones" type="number" min={1} max={60} required value={form.cantidadSesiones} onChange={(e) => setForm({ ...form, cantidadSesiones: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
            <label htmlFor="kineTelefonicoFechaInicio">Fecha de inicio</label>
            <input id="kineTelefonicoFechaInicio" type="date" required value={form.fechaInicio} onChange={(e) => setForm({ ...form, fechaInicio: e.target.value })} />
          </div>
        </div>
        <div className="field" style={{ marginBottom: 0 }} role="group" aria-labelledby="kineTelefonicoHorariosLabel">
          <label id="kineTelefonicoHorariosLabel">Horarios fijos semanales</label>
          {horarios.map((h) => (
            <div key={h.id} style={{ display: 'flex', gap: 8, marginBottom: 8 }}>
              <select value={h.diaSemana} onChange={(e) => setHorarios(horarios.map((x) => x.id === h.id ? { ...x, diaSemana: e.target.value } : x))}>
                {DIAS.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
              </select>
              <input type="time" value={h.horaInicio} onChange={(e) => setHorarios(horarios.map((x) => x.id === h.id ? { ...x, horaInicio: e.target.value } : x))} />
              {horarios.length > 1 && (
                <button type="button" className="btn btn-outline btn-sm" onClick={() => setHorarios(horarios.filter((x) => x.id !== h.id))}>Quitar</button>
              )}
            </div>
          ))}
          <button type="button" className="btn btn-outline btn-sm" onClick={() => setHorarios([...horarios, { id: crypto.randomUUID(), diaSemana: 'MONDAY', horaInicio: '15:00' }])}>
            + Agregar horario
          </button>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor="kineTelefonicoOrdenMedica">Orden médica (foto o PDF)</label>
          <input id="kineTelefonicoOrdenMedica" type="file" required accept="image/jpeg,image/png,application/pdf" onChange={(e) => setOrdenMedica(e.target.files[0] || null)} />
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button type="submit" className="btn btn-primary">Cargar plan</button>
          <button type="button" className="btn btn-outline" onClick={() => setAbierto(false)}>Cancelar</button>
        </div>
      </form>
    </div>
  );
}

function PlanRow({ plan }) {
  const dispatch = useDispatch();
  const confirmar = useConfirm();
  const [rechazando, setRechazando] = useState(false);
  const [motivo, setMotivo] = useState('');

  async function handleCancelarPlan() {
    const ok = await confirmar({
      titulo: 'Cancelar plan de kinesiología',
      detalle: `${plan.pacienteNombre} · ${plan.medicoNombre} · ${plan.sesionesGeneradas} de ${plan.cantidadSesiones} sesiones generadas.`,
      confirmarTexto: 'Cancelar plan',
      peligroso: true,
    });
    if (ok) dispatch(cancelarPlanKinesiologiaAdmin(plan.id));
  }

  return (
    <div className="card" style={{ marginBottom: 12 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 10 }}>
        <div>
          <div style={{ fontWeight: 600 }}>{plan.pacienteNombre} · DNI {plan.pacienteDni}</div>
          <div style={{ fontSize: 14, color: 'var(--muted)' }}>
            {plan.medicoNombre} · {plan.cantidadSesiones} sesiones · desde {plan.fechaInicio} · {plan.sesionesGeneradas} generadas
          </div>
        </div>
        <button className="btn btn-outline btn-sm" onClick={() => verOrdenMedica(plan.id)}>
          Ver orden médica
        </button>
      </div>

      {plan.estado === 'PENDIENTE_VALIDACION' && (
        <div style={{ marginTop: 12 }}>
          {!rechazando ? (
            <div style={{ display: 'flex', gap: 8 }}>
              <button className="btn btn-primary btn-sm" onClick={() => dispatch(validarPlanKinesiologia(plan.id))}>
                Validar y generar turnos
              </button>
              <button className="btn btn-outline btn-sm" onClick={() => setRechazando(true)}>
                Rechazar
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
              <input placeholder="Motivo del rechazo" value={motivo} onChange={(e) => setMotivo(e.target.value)} style={{ flex: 1, padding: '6px 10px', fontSize: 14 }} />
              <button className="btn btn-primary btn-sm" disabled={!motivo.trim()}
                onClick={() => { dispatch(rechazarPlanKinesiologia({ id: plan.id, motivo: motivo.trim() })); setRechazando(false); }}>
                Confirmar rechazo
              </button>
              <button className="btn btn-outline btn-sm" onClick={() => setRechazando(false)}>Cancelar</button>
            </div>
          )}
        </div>
      )}

      {(plan.estado === 'VALIDADO' || plan.estado === 'PAUSADO') && (
        <div style={{ marginTop: 12 }}>
          <button className="btn btn-outline btn-sm" onClick={handleCancelarPlan}>
            Cancelar plan
          </button>
        </div>
      )}
    </div>
  );
}

export default function AdminKinesiologiaTab() {
  const dispatch = useDispatch();
  const { planesAdmin, error, mensajeExito } = useSelector((state) => state.kinesiologia);
  const medicos = useSelector((state) => state.admin.medicos);
  const [estado, setEstado] = useState('PENDIENTE_VALIDACION');

  const medicosKinesiologia = medicos; // el backend ya valida que el médico sea de kinesiología al crear el plan

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
  }, [dispatch]);

  useEffect(() => {
    dispatch(fetchPlanesKinesiologiaAdmin(estado));
  }, [dispatch, estado]);

  return (
    <div>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 14 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginBottom: 14 }}>{mensajeExito}</div>}

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexWrap: 'wrap', gap: 12, marginBottom: 20 }}>
        <div className="field" style={{ marginBottom: 0, minWidth: 220 }}>
          <label htmlFor="kineFiltroEstado">Filtrar por estado</label>
          <select id="kineFiltroEstado" value={estado} onChange={(e) => { setEstado(e.target.value); dispatch(limpiarMensajesKinesiologia()); }}>
            {ESTADOS.map((e) => <option key={e.value} value={e.value}>{e.label}</option>)}
          </select>
        </div>
        <NuevoPlanTelefonico medicosKinesiologia={medicosKinesiologia} />
      </div>

      {planesAdmin.length === 0 && <EstadoVacio titulo="No hay planes en este estado" subtitulo="Probá con otro filtro o cargá un plan por teléfono." />}
      <div className="stagger">
        {planesAdmin.map((p) => <PlanRow key={p.id} plan={p} />)}
      </div>
    </div>
  );
}
