import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { fetchAuditoria } from './adminSlice';
import EstadoVacio from '../../components/EstadoVacio';

const TIPOS = [
  { value: '', label: 'Todos' },
  { value: 'TURNO', label: 'Turnos' },
  { value: 'MEDICO', label: 'Médicos' },
  { value: 'ESPECIALIDAD', label: 'Especialidades' },
  { value: 'INGRESO', label: 'Ingresos de caja' },
  { value: 'RETIRO', label: 'Retiros de caja' },
  { value: 'GASTO', label: 'Gastos' },
  { value: 'CUENTA_STAFF', label: 'Cuentas de staff' },
];

function formatoFecha(iso) {
  return new Date(iso).toLocaleString('es-AR');
}

export default function AdminAuditoriaTab() {
  const dispatch = useDispatch();
  const { auditoria, error } = useSelector((state) => state.admin);
  const [filtros, setFiltros] = useState({ tipoEntidad: '', desde: '', hasta: '' });

  useEffect(() => {
    dispatch(fetchAuditoria({ ...filtros, page: 0 }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dispatch]);

  function handleBuscar(e) {
    e.preventDefault();
    dispatch(fetchAuditoria({ ...filtros, page: 0 }));
  }

  function irAPagina(page) {
    dispatch(fetchAuditoria({ ...filtros, page }));
  }

  return (
    <div>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 14 }}>{error}</div>}

      <p style={{ fontSize: 13.5, color: 'var(--muted)', marginBottom: 16, maxWidth: 640 }}>
        Quién hizo qué y cuándo — incluye altas/bajas de cuentas de staff y movimientos de caja,
        además de turnos. Los cambios de médicos/especialidades y las anulaciones de caja quedan acá.
      </p>

      <form onSubmit={handleBuscar} className="filter-bar">
        <div className="field" style={{ minWidth: 180 }}>
          <label htmlFor="auditoriaTipo">Tipo</label>
          <select id="auditoriaTipo" value={filtros.tipoEntidad} onChange={(e) => setFiltros({ ...filtros, tipoEntidad: e.target.value })}>
            {TIPOS.map((t) => <option key={t.value} value={t.value}>{t.label}</option>)}
          </select>
        </div>
        <div className="field">
          <label htmlFor="auditoriaDesde">Desde</label>
          <input id="auditoriaDesde" type="date" value={filtros.desde} onChange={(e) => setFiltros({ ...filtros, desde: e.target.value })} />
        </div>
        <div className="field">
          <label htmlFor="auditoriaHasta">Hasta</label>
          <input id="auditoriaHasta" type="date" value={filtros.hasta} onChange={(e) => setFiltros({ ...filtros, hasta: e.target.value })} />
        </div>
        <button type="submit" className="btn btn-outline">Buscar</button>
      </form>

      {auditoria.content.length === 0 && <EstadoVacio titulo="Sin registros" subtitulo="No hay registros de auditoría para estos filtros." />}
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {auditoria.content.map((r) => (
          <div key={r.id} className="card">
            <div style={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 8 }}>
              <div>
                <strong>{r.operacion}</strong> · {r.tipoEntidad} #{r.entidadId}
                {r.deshecho && <span className="badge badge-cancelado" style={{ marginLeft: 8 }}>Deshecho</span>}
              </div>
              <div style={{ fontSize: 13, color: 'var(--muted)' }}>{formatoFecha(r.createdAt)}</div>
            </div>
            <div style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>Por: {r.usuarioNombre}</div>
          </div>
        ))}
      </div>

      {auditoria.totalPages > 1 && (
        <div style={{ display: 'flex', gap: 6, marginTop: 16, flexWrap: 'wrap' }}>
          {Array.from({ length: auditoria.totalPages }, (_, i) => i).map((p) => (
            <button key={p} className="btn btn-outline" disabled={p === auditoria.number}
              style={{ padding: '4px 10px', fontSize: 12 }} onClick={() => irAPagina(p)}>
              {p + 1}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
