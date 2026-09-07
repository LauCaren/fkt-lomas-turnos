import { useEffect, useState } from 'react';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

const ESTILO_POR_RESULTADO = {
  LLEGO: 'alert-success',
  REQUIERE_MOSTRADOR: 'alert-warning',
  SIN_TURNO_HOY: 'alert-error',
  YA_REGISTRADO: '',
};

export default function CheckInPage() {
  const [estado, setEstado] = useState('cargando');
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    axiosClient.post('/api/turnos/checkin')
      .then(({ data }) => {
        setResultado(data);
        setEstado('listo');
      })
      .catch((err) => {
        setError(mensajeDeError(err, 'No pudimos registrar tu llegada. Avisale a la secretaria en el mostrador.'));
        setEstado('error');
      });
  }, []);

  return (
    <div style={{ maxWidth: 420, margin: '60px auto', textAlign: 'center' }}>
      <span className="eyebrow">FKT Lomas</span>
      <h1 style={{ marginTop: 12, fontSize: 26 }}>Registro de llegada</h1>

      <div className="card" style={{ marginTop: 24 }}>
        {estado === 'cargando' && <p style={{ color: 'var(--muted)' }}>Registrando tu llegada…</p>}
        {estado === 'error' && <div className="alert alert-error">{error}</div>}
        {estado === 'listo' && (
          <>
            <div className={`alert ${ESTILO_POR_RESULTADO[resultado.resultado] || 'alert-error'}`}>
              {resultado.mensaje}
            </div>
            {resultado.medicoNombre && (
              <p style={{ color: 'var(--muted)', fontSize: 14 }}>
                {resultado.medicoNombre}{resultado.horaTurno ? ` · ${resultado.horaTurno.slice(0, 5)}` : ''}
              </p>
            )}
          </>
        )}
      </div>
    </div>
  );
}
