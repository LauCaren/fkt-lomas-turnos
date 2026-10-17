import { useEffect, useState } from 'react';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';
import { descargarArchivo } from '../../api/descargas';
import { hoyISO } from '../../utils/formato';

function primerDiaMesISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

function ExportarSeccion({ titulo, descripcion, endpoint, prefijoArchivo }) {
  const [desde, setDesde] = useState(primerDiaMesISO());
  const [hasta, setHasta] = useState(hoyISO());
  const [descargando, setDescargando] = useState(null);
  const [error, setError] = useState(null);

  function handleExportar(formato) {
    setDescargando(formato);
    setError(null);
    descargarArchivo(endpoint, { desde, hasta, formato }, `${prefijoArchivo}_${desde}_a_${hasta}.${formato === 'pdf' ? 'pdf' : 'xlsx'}`)
      .catch((err) => setError(mensajeDeError(err, 'No se pudo generar el archivo')))
      .finally(() => setDescargando(null));
  }

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <h3 style={{ fontSize: 16, marginBottom: 6 }}>{titulo}</h3>
      <p style={{ fontSize: 14, color: 'var(--muted)', marginBottom: 14 }}>{descripcion}</p>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 12 }}>{error}</div>}
      <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor={`reporteDesde-${prefijoArchivo}`}>Desde</label>
          <input id={`reporteDesde-${prefijoArchivo}`} type="date" value={desde} onChange={(e) => setDesde(e.target.value)} />
        </div>
        <div className="field" style={{ marginBottom: 0 }}>
          <label htmlFor={`reporteHasta-${prefijoArchivo}`}>Hasta</label>
          <input id={`reporteHasta-${prefijoArchivo}`} type="date" value={hasta} onChange={(e) => setHasta(e.target.value)} />
        </div>
        <button className="btn btn-outline" disabled={!!descargando} onClick={() => handleExportar('excel')}>
          {descargando === 'excel' && <span className="spinner" aria-hidden="true" />}
          {descargando === 'excel' ? 'Generando…' : 'Descargar Excel'}
        </button>
        <button className="btn btn-outline" disabled={!!descargando} onClick={() => handleExportar('pdf')}>
          {descargando === 'pdf' && <span className="spinner" aria-hidden="true" />}
          {descargando === 'pdf' ? 'Generando…' : 'Descargar PDF'}
        </button>
      </div>
    </div>
  );
}

function ConfiguracionReporteMensual() {
  const [email, setEmail] = useState('');
  const [guardando, setGuardando] = useState(false);
  const [mensaje, setMensaje] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    axiosClient.get('/api/admin/caja/configuracion')
      .then(({ data }) => setEmail(data.emailReporteMensual || ''))
      .catch((err) => setError(mensajeDeError(err)));
  }, []);

  function handleSubmit(e) {
    e.preventDefault();
    setGuardando(true);
    setMensaje(null);
    setError(null);
    axiosClient.put('/api/admin/caja/configuracion', { emailReporteMensual: email || null })
      .then(() => setMensaje('Guardado correctamente'))
      .catch((err) => setError(mensajeDeError(err)))
      .finally(() => setGuardando(false));
  }

  return (
    <div className="card">
      <h3 style={{ fontSize: 16, marginBottom: 6 }}>Envío mensual automático de caja</h3>
      <p style={{ fontSize: 14, color: 'var(--muted)', marginBottom: 14 }}>
        El último día de cada mes se manda automáticamente un Excel con todos los ingresos, retiros y gastos del mes
        a esta dirección. Dejalo vacío para desactivar el envío.
      </p>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 12 }}>{error}</div>}
      {mensaje && <div className="alert alert-success" role="status" style={{ marginBottom: 12 }}>{mensaje}</div>}
      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 10, alignItems: 'flex-end', flexWrap: 'wrap' }}>
        <div className="field" style={{ marginBottom: 0, flex: '1 1 260px' }}>
          <label htmlFor="reporteEmailDestino">Email de destino</label>
          <input id="reporteEmailDestino" type="email" placeholder="contador@ejemplo.com" value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <button type="submit" className="btn btn-primary" disabled={guardando}>
          {guardando && <span className="spinner" aria-hidden="true" />}
          {guardando ? 'Guardando…' : 'Guardar'}
        </button>
      </form>
    </div>
  );
}

export default function AdminReportesTab() {
  return (
    <div>
      <ExportarSeccion
        titulo="Agenda / movimientos del consultorio"
        descripcion="Turnos (médico, paciente, estado, motivo) en el rango elegido."
        endpoint="/api/admin/reportes/agenda"
        prefijoArchivo="agenda"
      />
      <ExportarSeccion
        titulo="Caja"
        descripcion="Ingresos, retiros y gastos en el rango elegido, con un resumen al final."
        endpoint="/api/admin/reportes/caja"
        prefijoArchivo="caja"
      />
      <ConfiguracionReporteMensual />
    </div>
  );
}
