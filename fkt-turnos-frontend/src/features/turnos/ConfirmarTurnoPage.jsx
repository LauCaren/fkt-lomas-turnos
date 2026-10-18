import { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';
import Resultado from '../../components/ui/Resultado';

// Página a la que lleva el link del recordatorio ("¿Venís a tu turno? Sí / No"), sin login: el
// token del link es la credencial.
//
// "Sí" se registra apenas abre la página: es inofensivo y le ahorra un paso a quien ya dijo que
// viene. "No" en cambio pide un clic más antes de cancelar. Los antivirus de correo (Outlook,
// algunos de empresas) abren solos los links de los mails para revisarlos, a veces en un
// navegador real: si "No" cancelaba al cargar, uno de esos escaneos podía liberar el turno de
// alguien sin que la persona hiciera nada. La respuesta se manda por POST por la misma razón
// (un GET no debería cambiar datos).
export default function ConfirmarTurnoPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const respuesta = searchParams.get('respuesta');
  const linkValido = Boolean(token) && (respuesta === 'si' || respuesta === 'no');

  // preguntar → (clic) → cargando → listo | error
  const [estado, setEstado] = useState(() => {
    if (!linkValido) return 'error';
    return respuesta === 'si' ? 'cargando' : 'preguntar';
  });
  const [confirmando, setConfirmando] = useState(respuesta === 'si');
  const [error, setError] = useState(linkValido ? null : 'El link no es válido.');

  // Solo actualiza el estado cuando responde el backend (no de forma sincrónica): así se puede
  // llamar desde el efecto de abajo sin provocar un render extra.
  const enviar = useCallback((viene) => axiosClient
    .post('/api/turnos/confirmacion', null, { params: { token, respuesta: viene ? 'si' : 'no' } })
    .then(() => setEstado('listo'))
    .catch((err) => {
      setEstado('error');
      setError(mensajeDeError(err, 'El link no es válido o ya venció'));
    }), [token]);

  function responder(viene) {
    setConfirmando(viene);
    setEstado('cargando');
    enviar(viene);
  }

  // "Sí" arranca ya en estado "cargando" (ver useState de arriba): acá solo se manda.
  useEffect(() => {
    if (linkValido && respuesta === 'si') enviar(true);
  }, [linkValido, respuesta, enviar]);

  if (estado === 'preguntar') {
    return (
      <Resultado estado="aviso" titulo="¿No vas a poder venir?">
        <p>Si cancelás, el horario queda libre para otro paciente. Si querés venir otro día, después podés sacar un turno nuevo.</p>
        <div className="resultado-acciones">
          <button type="button" className="btn btn-outline btn-peligro" onClick={() => responder(false)}>
            Sí, cancelar mi turno
          </button>
          <button type="button" className="btn btn-primary" onClick={() => responder(true)}>
            Me equivoqué, voy a ir
          </button>
        </div>
      </Resultado>
    );
  }
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
