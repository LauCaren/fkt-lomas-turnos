import { useEffect, useState } from 'react';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';
import Resultado from '../../components/ui/Resultado';

const SEGUN_RESULTADO = {
  LLEGO: { estado: 'ok', titulo: '¡Listo, ya sabemos que llegaste!' },
  YA_REGISTRADO: { estado: 'ok', titulo: 'Tu llegada ya estaba registrada' },
  REQUIERE_MOSTRADOR: { estado: 'aviso', titulo: 'Pasá por el mostrador' },
  SIN_TURNO_HOY: { estado: 'error', titulo: 'No encontramos un turno tuyo para hoy' },
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

  if (estado === 'cargando') {
    return <Resultado estado="cargando" titulo="Registrando tu llegada…" />;
  }
  if (estado === 'error') {
    return (
      <Resultado estado="error" titulo="No pudimos registrar tu llegada">
        <p role="alert">{error}</p>
      </Resultado>
    );
  }

  const segun = SEGUN_RESULTADO[resultado.resultado] || { estado: 'error', titulo: 'Avisale a la secretaria' };
  return (
    <Resultado estado={segun.estado} titulo={segun.titulo}>
      <p role="status">{resultado.mensaje}</p>
      {resultado.medicoNombre && (
        <p className="resultado-turno">
          {resultado.medicoNombre}{resultado.horaTurno ? <> · <strong>{resultado.horaTurno.slice(0, 5)}</strong></> : null}
        </p>
      )}
    </Resultado>
  );
}
