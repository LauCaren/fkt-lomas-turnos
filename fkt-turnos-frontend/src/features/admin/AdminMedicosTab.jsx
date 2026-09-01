import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchMedicosAdmin, crearMedico, actualizarMedico, crearEspecialidad,
  fetchReglas, crearRegla, eliminarRegla, generarTurnos, subirFotoMedico, subirTituloMedico,
  crearOActualizarCredencialesMedico,
} from './adminSlice';
import { fetchEspecialidades } from '../turnos/turnosSlice';
import Lightbox from '../../components/Lightbox';

const DIAS = [
  { value: 'MONDAY', label: 'Lunes' },
  { value: 'TUESDAY', label: 'Martes' },
  { value: 'WEDNESDAY', label: 'Miércoles' },
  { value: 'THURSDAY', label: 'Jueves' },
  { value: 'FRIDAY', label: 'Viernes' },
];

function hoyISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function CredencialesMedico({ medico }) {
  const dispatch = useDispatch();
  const [form, setForm] = useState({ email: medico.usuarioEmail || '', password: '' });
  const [enviando, setEnviando] = useState(false);

  function handleSubmit(e) {
    e.preventDefault();
    setEnviando(true);
    dispatch(crearOActualizarCredencialesMedico({ medicoId: medico.id, payload: form })).unwrap()
      .then(() => setForm({ ...form, password: '' }))
      .finally(() => setEnviando(false));
  }

  return (
    <div style={{ marginBottom: 20 }}>
      <h4 style={{ fontSize: 15, marginBottom: 6 }}>Acceso al panel (rol Médico)</h4>
      <p style={{ fontSize: 12.5, color: 'var(--muted)', marginBottom: 10 }}>
        {medico.usuarioEmail
          ? `Ya tiene acceso con ${medico.usuarioEmail}. Completá los campos para cambiar el email o resetear la contraseña.`
          : 'Este médico todavía no tiene usuario para loguearse en el panel. Cargá un email y una contraseña inicial.'}
      </p>
      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 220px' }}>
          <label>Email</label>
          <input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        </div>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 220px' }}>
          <label>{medico.usuarioEmail ? 'Nueva contraseña' : 'Contraseña inicial'}</label>
          <input type="password" required value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-outline" disabled={enviando}>{enviando ? 'Guardando…' : 'Guardar credenciales'}</button>
      </form>
    </div>
  );
}

function MedicoDetalle({ medico }) {
  const dispatch = useDispatch();
  const reglas = useSelector((state) => state.admin.reglasPorMedico[medico.id] || []);
  const [nuevaRegla, setNuevaRegla] = useState({ diaSemana: 'MONDAY', horaInicio: '08:00', horaFin: '12:00' });
  const [rango, setRango] = useState({ desde: hoyISO(), hasta: hoyISO() });
  const [anosExperiencia, setAnosExperiencia] = useState(medico.anosExperiencia ?? '');
  const [tarifaConsulta, setTarifaConsulta] = useState(medico.tarifaConsulta ?? '');

  function payloadBase() {
    return {
      nombreCompleto: medico.nombreCompleto,
      especialidadId: medico.especialidadId,
      matricula: medico.matricula,
      activo: medico.activo,
      duracionTurnoMinutos: medico.duracionTurnoMinutos,
      anosExperiencia: medico.anosExperiencia,
      cobraCopagoIoma: medico.cobraCopagoIoma,
      tarifaConsulta: medico.tarifaConsulta,
    };
  }

  function handleGuardarAnosExperiencia(e) {
    e.preventDefault();
    dispatch(actualizarMedico({
      id: medico.id,
      payload: { ...payloadBase(), anosExperiencia: anosExperiencia === '' ? null : Number(anosExperiencia) },
    }));
  }

  function handleToggleCopagoIoma(e) {
    dispatch(actualizarMedico({ id: medico.id, payload: { ...payloadBase(), cobraCopagoIoma: e.target.checked } }));
  }

  function handleGuardarTarifa(e) {
    e.preventDefault();
    dispatch(actualizarMedico({
      id: medico.id,
      payload: { ...payloadBase(), tarifaConsulta: tarifaConsulta === '' ? null : Number(tarifaConsulta) },
    }));
  }

  useEffect(() => {
    dispatch(fetchReglas(medico.id));
  }, [medico.id, dispatch]);

  function handleAgregarRegla(e) {
    e.preventDefault();
    dispatch(crearRegla({ medicoId: medico.id, payload: nuevaRegla }));
  }

  function handleGenerar(e) {
    e.preventDefault();
    dispatch(generarTurnos({ medicoId: medico.id, ...rango }));
  }

  const apiUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080';
  const [fotoVersion, setFotoVersion] = useState(0);
  const [tituloVersion, setTituloVersion] = useState(0);
  const [lightboxSrc, setLightboxSrc] = useState(null);

  function handleSubirFoto(e) {
    const file = e.target.files[0];
    if (file) {
      dispatch(subirFotoMedico({ medicoId: medico.id, file })).unwrap()
        .then(() => setFotoVersion((v) => v + 1)).catch(() => {});
    }
    e.target.value = '';
  }

  function handleSubirTitulo(e) {
    const file = e.target.files[0];
    if (file) {
      dispatch(subirTituloMedico({ medicoId: medico.id, file })).unwrap()
        .then(() => setTituloVersion((v) => v + 1)).catch(() => {});
    }
    e.target.value = '';
  }

  return (
    <div style={{ padding: '18px 0 4px', borderTop: '1px solid var(--line)', marginTop: 14 }}>
      <CredencialesMedico medico={medico} />

      <h4 style={{ fontSize: 15, marginBottom: 10 }}>Ficha pública (página "Equipo")</h4>
      <form onSubmit={handleGuardarAnosExperiencia} style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginBottom: 16 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Años de experiencia</label>
          <input type="number" min={0} max={70} value={anosExperiencia} onChange={(e) => setAnosExperiencia(e.target.value)} style={{ width: 100 }} />
        </div>
        <button type="submit" className="btn btn-outline">Guardar</button>
      </form>
      <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap', marginBottom: 20 }}>
        <div>
          <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Foto (tipo carnet)</label>
          {medico.tieneFoto && (
            <img src={`${apiUrl}/api/medicos/${medico.id}/foto?v=${fotoVersion}`} alt=""
              onClick={() => setLightboxSrc(`${apiUrl}/api/medicos/${medico.id}/foto?v=${fotoVersion}`)}
              style={{ width: 90, height: 110, objectFit: 'cover', borderRadius: 6, border: '1px solid var(--line)', marginBottom: 8, display: 'block', cursor: 'zoom-in' }} />
          )}
          <input type="file" accept="image/jpeg,image/png" onChange={handleSubirFoto} style={{ fontSize: 12.5 }} />
        </div>
        <div>
          <label style={{ display: 'block', fontSize: 13, fontWeight: 600, marginBottom: 6 }}>Título / matrícula (imagen)</label>
          {medico.tieneTitulo && (
            <img src={`${apiUrl}/api/medicos/${medico.id}/titulo?v=${tituloVersion}`} alt=""
              onClick={() => setLightboxSrc(`${apiUrl}/api/medicos/${medico.id}/titulo?v=${tituloVersion}`)}
              style={{ width: 140, height: 100, objectFit: 'cover', borderRadius: 6, border: '1px solid var(--line)', marginBottom: 8, display: 'block', cursor: 'zoom-in' }} />
          )}
          <input type="file" accept="image/jpeg,image/png" onChange={handleSubirTitulo} style={{ fontSize: 12.5 }} />
        </div>
      </div>
      <Lightbox src={lightboxSrc} alt={medico.nombreCompleto} onClose={() => setLightboxSrc(null)} />

      <h4 style={{ fontSize: 15, marginBottom: 10 }}>Obra social IOMA</h4>
      <label style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14, marginBottom: 20 }}>
        <input type="checkbox" checked={!!medico.cobraCopagoIoma} onChange={handleToggleCopagoIoma} />
        Este médico cobra copago a pacientes con IOMA
      </label>

      <h4 style={{ fontSize: 15, marginBottom: 10 }}>Tarifa de consulta</h4>
      <form onSubmit={handleGuardarTarifa} style={{ display: 'flex', gap: 8, alignItems: 'flex-end', marginBottom: 20 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Monto (se autocompleta al cobrar en caja)</label>
          <input type="number" min={0} step="0.01" value={tarifaConsulta} onChange={(e) => setTarifaConsulta(e.target.value)} style={{ width: 140 }} />
        </div>
        <button type="submit" className="btn btn-outline">Guardar</button>
      </form>

      <h4 style={{ fontSize: 15, marginBottom: 10 }}>Horarios semanales</h4>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 14 }}>
        {reglas.length === 0 && <p style={{ fontSize: 13, color: 'var(--muted)' }}>Sin horarios cargados todavía.</p>}
        {reglas.map((r) => (
          <div key={r.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: 14 }}>
            <span>{DIAS.find((d) => d.value === r.diaSemana)?.label} · {r.horaInicio.slice(0, 5)} a {r.horaFin.slice(0, 5)}</span>
            <button className="btn btn-outline" style={{ padding: '4px 10px', fontSize: 12 }}
              onClick={() => dispatch(eliminarRegla({ medicoId: medico.id, reglaId: r.id }))}>Quitar</button>
          </div>
        ))}
      </div>

      <form onSubmit={handleAgregarRegla} style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end', marginBottom: 20 }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Día</label>
          <select value={nuevaRegla.diaSemana} onChange={(e) => setNuevaRegla({ ...nuevaRegla, diaSemana: e.target.value })}>
            {DIAS.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
          </select>
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Desde</label>
          <input type="time" min="08:00" max="20:00" value={nuevaRegla.horaInicio} onChange={(e) => setNuevaRegla({ ...nuevaRegla, horaInicio: e.target.value })} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Hasta</label>
          <input type="time" min="08:00" max="20:00" value={nuevaRegla.horaFin} onChange={(e) => setNuevaRegla({ ...nuevaRegla, horaFin: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-outline">Agregar horario</button>
      </form>

      <h4 style={{ fontSize: 15, marginBottom: 10 }}>Generar turnos a partir de los horarios</h4>
      <form onSubmit={handleGenerar} style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Desde</label>
          <input type="date" value={rango.desde} onChange={(e) => setRango({ ...rango, desde: e.target.value })} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label>Hasta</label>
          <input type="date" value={rango.hasta} onChange={(e) => setRango({ ...rango, hasta: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-primary">Generar turnos</button>
      </form>
    </div>
  );
}

export default function AdminMedicosTab() {
  const dispatch = useDispatch();
  const { medicos, error, mensajeExito } = useSelector((state) => state.admin);
  const especialidades = useSelector((state) => state.turnos.especialidades);
  const [expandidoId, setExpandidoId] = useState(null);
  const [nuevaEspecialidad, setNuevaEspecialidad] = useState('');
  const [nuevoMedico, setNuevoMedico] = useState({ nombreCompleto: '', especialidadId: '', matricula: '', duracionTurnoMinutos: 30, anosExperiencia: '' });

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
    dispatch(fetchEspecialidades());
  }, [dispatch]);

  function handleCrearEspecialidad(e) {
    e.preventDefault();
    if (!nuevaEspecialidad.trim()) return;
    dispatch(crearEspecialidad(nuevaEspecialidad.trim())).unwrap().then(() => {
      setNuevaEspecialidad('');
      dispatch(fetchEspecialidades());
    }).catch(() => {});
  }

  function handleCrearMedico(e) {
    e.preventDefault();
    const payload = { ...nuevoMedico, anosExperiencia: nuevoMedico.anosExperiencia === '' ? null : Number(nuevoMedico.anosExperiencia) };
    dispatch(crearMedico(payload)).unwrap().then(() => {
      setNuevoMedico({ nombreCompleto: '', especialidadId: '', matricula: '', duracionTurnoMinutos: 30, anosExperiencia: '' });
    }).catch(() => {});
  }

  function handleToggleActivo(medico) {
    dispatch(actualizarMedico({
      id: medico.id,
      payload: {
        nombreCompleto: medico.nombreCompleto,
        especialidadId: medico.especialidadId,
        matricula: medico.matricula,
        activo: !medico.activo,
        duracionTurnoMinutos: medico.duracionTurnoMinutos,
        anosExperiencia: medico.anosExperiencia,
        cobraCopagoIoma: medico.cobraCopagoIoma,
        tarifaConsulta: medico.tarifaConsulta,
      },
    }));
  }

  return (
    <div>
      {error && <div className="alert alert-error">{error}</div>}
      {mensajeExito && <div className="alert alert-success">{mensajeExito}</div>}

      <div className="card" style={{ marginBottom: 20 }}>
        <h3 style={{ fontSize: 17, marginBottom: 14 }}>Nueva especialidad</h3>
        <form onSubmit={handleCrearEspecialidad} style={{ display: 'flex', gap: 8 }}>
          <input placeholder="Ej: Cardiología" value={nuevaEspecialidad} onChange={(e) => setNuevaEspecialidad(e.target.value)} style={{ flex: 1, padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6 }} />
          <button type="submit" className="btn btn-outline">Agregar</button>
        </form>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h3 style={{ fontSize: 17, marginBottom: 14 }}>Nuevo médico</h3>
        <form onSubmit={handleCrearMedico}>
          <div className="field">
            <label>Nombre completo</label>
            <input required value={nuevoMedico.nombreCompleto} onChange={(e) => setNuevoMedico({ ...nuevoMedico, nombreCompleto: e.target.value })} />
          </div>
          <div className="field">
            <label>Especialidad</label>
            <select required value={nuevoMedico.especialidadId} onChange={(e) => setNuevoMedico({ ...nuevoMedico, especialidadId: e.target.value })}>
              <option value="">Seleccioná una especialidad</option>
              {especialidades.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
            </select>
          </div>
          <div className="field">
            <label>Matrícula (opcional)</label>
            <input value={nuevoMedico.matricula} onChange={(e) => setNuevoMedico({ ...nuevoMedico, matricula: e.target.value })} />
          </div>
          <div className="field">
            <label>Duración del turno (minutos)</label>
            <input type="number" min={10} step={5} value={nuevoMedico.duracionTurnoMinutos} onChange={(e) => setNuevoMedico({ ...nuevoMedico, duracionTurnoMinutos: Number(e.target.value) })} />
          </div>
          <div className="field">
            <label>Años de experiencia (opcional, para la ficha pública)</label>
            <input type="number" min={0} max={70} value={nuevoMedico.anosExperiencia} onChange={(e) => setNuevoMedico({ ...nuevoMedico, anosExperiencia: e.target.value })} />
          </div>
          <button type="submit" className="btn btn-primary">Crear médico</button>
        </form>
      </div>

      <h3 style={{ fontSize: 17, marginBottom: 14 }}>Médicos cargados</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        {medicos.map((m) => (
          <div key={m.id} className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
              <div>
                <div style={{ fontWeight: 600 }}>{m.nombreCompleto}</div>
                <div style={{ fontSize: 13, color: 'var(--muted)' }}>{m.especialidadNombre} · turnos de {m.duracionTurnoMinutos} min {!m.activo && '· inactivo'}</div>
              </div>
              <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => handleToggleActivo(m)}>
                  {m.activo ? 'Desactivar' : 'Activar'}
                </button>
                <button className="btn btn-outline" style={{ padding: '6px 14px', fontSize: 13 }} onClick={() => setExpandidoId(expandidoId === m.id ? null : m.id)}>
                  {expandidoId === m.id ? 'Ocultar' : 'Gestionar horarios'}
                </button>
              </div>
            </div>
            {expandidoId === m.id && <MedicoDetalle medico={m} />}
          </div>
        ))}
      </div>
    </div>
  );
}
