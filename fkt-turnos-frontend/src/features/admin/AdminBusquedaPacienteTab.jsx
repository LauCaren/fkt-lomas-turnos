import { useState } from 'react';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

function formatMoneda(valor) {
  return Number(valor || 0).toLocaleString('es-AR', { style: 'currency', currency: 'ARS' });
}

const ESTADO_KINESIOLOGIA = {
  PENDIENTE_VALIDACION: 'Pendiente de revisión',
  VALIDADO: 'Activo',
  RECHAZADO: 'Rechazado',
  PAUSADO: 'Pausado',
  CANCELADO: 'Cancelado',
};

function Ficha({ ficha }) {
  return (
    <div style={{ marginTop: 24 }}>
      <div className="card" style={{ marginBottom: 20 }}>
        <div style={{ fontWeight: 600, fontSize: 17 }}>{ficha.paciente.nombreCompleto}</div>
        <div style={{ fontSize: 13.5, color: 'var(--muted)', marginTop: 4 }}>
          DNI {ficha.paciente.dni} · {ficha.paciente.telefono} · avisos por {ficha.paciente.canalNotificacionPreferido}
        </div>
      </div>

      <h3 style={{ fontSize: 16, marginBottom: 10 }}>Turnos ({ficha.turnos.length})</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 24 }}>
        {ficha.turnos.length === 0 && <p style={{ color: 'var(--muted)', fontSize: 13.5 }}>Sin turnos.</p>}
        {ficha.turnos.slice(0, 30).map((t) => (
          <div key={t.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', fontSize: 13.5 }}>
            <span>{t.fecha} {t.horaInicio.slice(0, 5)} · {t.medicoNombre} ({t.especialidadNombre})</span>
            <span className={`badge badge-${t.estado === 'DISPONIBLE' ? 'disponible' : t.estado === 'RESERVADO' ? 'reservado' : 'cancelado'}`}>{t.estado}</span>
          </div>
        ))}
      </div>

      <h3 style={{ fontSize: 16, marginBottom: 10 }}>Planes de kinesiología ({ficha.planesKinesiologia.length})</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 24 }}>
        {ficha.planesKinesiologia.length === 0 && <p style={{ color: 'var(--muted)', fontSize: 13.5 }}>Sin planes.</p>}
        {ficha.planesKinesiologia.map((p) => (
          <div key={p.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', fontSize: 13.5 }}>
            <span>{p.medicoNombre} · {p.sesionesGeneradas} de {p.cantidadSesiones} sesiones</span>
            <span className="badge">{ESTADO_KINESIOLOGIA[p.estado] || p.estado}</span>
          </div>
        ))}
      </div>

      <h3 style={{ fontSize: 16, marginBottom: 10 }}>Cobros asociados ({ficha.ingresos.length})</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {ficha.ingresos.length === 0 && <p style={{ color: 'var(--muted)', fontSize: 13.5 }}>Sin cobros asociados a un turno de este paciente.</p>}
        {ficha.ingresos.map((i) => (
          <div key={i.id} className="card" style={{ padding: '10px 16px', display: 'flex', justifyContent: 'space-between', fontSize: 13.5, opacity: i.anulado ? 0.55 : 1 }}>
            <span>{i.fecha} · {i.medicoNombre} · {i.metodoPago}</span>
            <strong>{formatMoneda(i.monto)}</strong>
          </div>
        ))}
      </div>
    </div>
  );
}

export default function AdminBusquedaPacienteTab() {
  const [busqueda, setBusqueda] = useState('');
  const [resultados, setResultados] = useState([]);
  const [ficha, setFicha] = useState(null);
  const [error, setError] = useState(null);
  const [cargando, setCargando] = useState(false);

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
        <button type="submit" className="btn btn-primary" disabled={cargando}>Buscar</button>
      </form>

      {error && <div className="alert alert-error">{error}</div>}

      {!ficha && resultados.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {resultados.map((p) => (
            <button
              key={p.id}
              className="card"
              onClick={() => verFicha(p.id)}
              style={{ textAlign: 'left', padding: '12px 18px', cursor: 'pointer', display: 'flex', justifyContent: 'space-between', border: 'none' }}
            >
              <span><strong>{p.nombreCompleto}</strong> · DNI {p.dni}</span>
              <span style={{ color: 'var(--muted)' }}>{p.telefono}</span>
            </button>
          ))}
        </div>
      )}

      {ficha && (
        <>
          <button className="btn btn-outline" style={{ marginBottom: 8 }} onClick={() => setFicha(null)}>← Volver a resultados</button>
          <Ficha ficha={ficha} />
        </>
      )}
    </div>
  );
}
