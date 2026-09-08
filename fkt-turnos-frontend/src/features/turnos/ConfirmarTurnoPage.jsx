import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

export default function ConfirmarTurnoPage() {
  const [searchParams] = useSearchParams();
  const [estado, setEstado] = useState('cargando');
  const [error, setError] = useState(null);

  const token = searchParams.get('token');
  const respuesta = searchParams.get('respuesta');
  const confirmando = respuesta === 'si';

  useEffect(() => {
    if (!token || !respuesta) {
      setEstado('error');
      setError('El link no es válido.');
      return;
    }
    axiosClient.get('/api/turnos/confirmacion', { params: { token, respuesta } })
      .then(() => setEstado('listo'))
      .catch((err) => {
        setEstado('error');
        setError(mensajeDeError(err, 'El link no es válido o ya venció'));
      });
  }, [token, respuesta]);

  return (
    <div style={{ maxWidth: 420, margin: '80px auto', textAlign: 'center' }}>
      <span className="eyebrow">FKT Lomas</span>
      <h1 style={{ marginTop: 12, fontSize: 26 }}>Confirmación de turno</h1>

      <div className="card" style={{ marginTop: 24 }}>
        {estado === 'cargando' && <p style={{ color: 'var(--muted)' }}>Procesando…</p>}
        {estado === 'error' && <div className="alert alert-error" role="alert">{error}</div>}
        {estado === 'listo' && confirmando && (
          <div className="alert alert-success" role="status">¡Gracias! Confirmamos que vas a venir a tu turno.</div>
        )}
        {estado === 'listo' && !confirmando && (
          <div className="alert" style={{ background: 'var(--red-tint)', color: 'var(--red)', border: '1px solid var(--red)' }}>
            Listo, avisamos a la clínica que no vas a poder venir. El horario queda libre para otro paciente.
          </div>
        )}
      </div>
    </div>
  );
}
