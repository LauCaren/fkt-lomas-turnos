import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';
import Resultado from '../../components/ui/Resultado';

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

  if (estado === 'cargando') {
    return <Resultado estado="cargando" titulo="Un segundo…" />;
  }
  if (estado === 'error') {
    return (
      <Resultado estado="error" titulo="No pudimos procesar tu respuesta">
        <p role="alert">{error}</p>
        <p>Si necesitás confirmar o cancelar, escribinos o llamanos y lo resolvemos.</p>
      </Resultado>
    );
  }
  return confirmando ? (
    <Resultado estado="ok" titulo="¡Gracias! Te esperamos">
      <p role="status">Confirmamos que vas a venir a tu turno.</p>
    </Resultado>
  ) : (
    <Resultado estado="aviso" titulo="Listo, liberamos tu turno">
      <p role="status">Avisamos al centro que no vas a poder venir. El horario queda libre para otro paciente.</p>
    </Resultado>
  );
}
