import { useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import axiosClient from '../api/axiosClient';
import { solicitarBajaCuenta, limpiarMensajesCuenta } from '../features/cuenta/cuentaSlice';

function descargarJson(objeto, nombreArchivo) {
  const blob = new Blob([JSON.stringify(objeto, null, 2)], { type: 'application/json' });
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', nombreArchivo);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(url);
}

function ExportarDatos() {
  const [descargando, setDescargando] = useState(false);

  async function handleExportar() {
    setDescargando(true);
    try {
      const { data } = await axiosClient.get('/api/mi-cuenta/exportar-datos');
      descargarJson(data, `mis-datos-fkt-lomas-${new Date().toISOString().slice(0, 10)}.json`);
    } catch {
      // El error ya queda logueado por axiosClient; no hay estado de redux involucrado acá.
    } finally {
      setDescargando(false);
    }
  }

  return (
    <div className="card" style={{ marginBottom: 16 }}>
      <p style={{ fontSize: 14, marginBottom: 12 }}>
        Descargá un archivo con todos los datos que tenemos sobre tu cuenta: perfil, turnos, planes de
        kinesiología e historia clínica.
      </p>
      <button type="button" className="btn btn-outline" disabled={descargando} onClick={handleExportar}>
        {descargando ? 'Generando…' : 'Exportar mis datos'}
      </button>
    </div>
  );
}

function SolicitarBaja() {
  const dispatch = useDispatch();
  const { solicitudBaja, mensajeExito } = useSelector((state) => state.cuenta);
  const [motivo, setMotivo] = useState('');
  const [confirmando, setConfirmando] = useState(false);

  function handleSolicitar(e) {
    e.preventDefault();
    dispatch(limpiarMensajesCuenta());
    dispatch(solicitarBajaCuenta(motivo)).unwrap().then(() => setConfirmando(false)).catch(() => {});
  }

  if (solicitudBaja || (mensajeExito && mensajeExito.includes('pedido de baja'))) {
    return (
      <div className="card">
        <p style={{ fontSize: 14 }}>
          Ya tenés un pedido de baja registrado. El centro lo va a revisar y te va a avisar por email cuando
          se procese.
        </p>
      </div>
    );
  }

  return (
    <div className="card">
      <p style={{ fontSize: 14, marginBottom: 12 }}>
        Al aprobarse, tus datos personales (nombre, email, teléfono, DNI) se eliminan de forma permanente.
        Tus turnos y registros ya generados se conservan de forma anónima, como exige la normativa de
        historia clínica — no vamos a poder identificarte en ellos.
      </p>
      {!confirmando ? (
        <button type="button" className="btn btn-outline" onClick={() => setConfirmando(true)}>Solicitar baja de mi cuenta</button>
      ) : (
        <form onSubmit={handleSolicitar}>
          <div className="field">
            <label>Motivo (opcional)</label>
            <textarea rows={2} value={motivo} onChange={(e) => setMotivo(e.target.value)}
              style={{ width: '100%', padding: '10px 14px', border: '1.5px solid var(--line)', borderRadius: 6, fontFamily: 'inherit' }} />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" className="btn btn-primary">Confirmar pedido de baja</button>
            <button type="button" className="btn btn-outline" onClick={() => setConfirmando(false)}>Cancelar</button>
          </div>
        </form>
      )}
    </div>
  );
}

/** Derechos de datos personales (Ley 25.326): exportar y solicitar baja. Solo tiene sentido para pacientes — el backend rechaza la baja para cuentas de staff (ver SolicitudBajaService). */
export default function SeccionPrivacidad() {
  return (
    <div>
      <ExportarDatos />
      <SolicitarBaja />
    </div>
  );
}
