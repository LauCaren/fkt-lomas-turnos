import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  fetchEspecialidades, fetchMedicos, fetchTurnosDisponibles, reservarTurno, cancelarTurno, limpiarReserva, limpiarDisponibles,
  fetchMiAnotacionListaEspera, anotarseListaEspera, darseDeBajaListaEspera, fetchProximaDisponibilidad,
  fetchMercadoPagoEstado, pagarTurnoOnline,
} from './turnosSlice';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import AgregarCalendario from '../../components/AgregarCalendario';
import { hoyISO, formatFechaCorta } from '../../utils/formato';

const WHATSAPP_CONSULTORIO = { numero: '5491123515402', texto: 'Hola, quiero confirmar mi turno con copago IOMA.' };

export default function BookingWizard() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();
  const {
    especialidades, medicos, disponibles, status, error, reservaExitosa, miAnotacionListaEspera, proximaDisponibilidad,
    mercadoPagoHabilitado, pagoOnlineCargando, pagoOnlineError,
  } = useSelector((state) => state.turnos);
  const perfil = useSelector((state) => state.cuenta.perfil);
  const [anotandoListaEspera, setAnotandoListaEspera] = useState(false);

  const turnoAReprogramar = location.state?.reprogramar || null;

  const [especialidadId, setEspecialidadId] = useState(turnoAReprogramar?.especialidadId ? String(turnoAReprogramar.especialidadId) : '');
  const [medicoId, setMedicoId] = useState('');
  const [fecha, setFecha] = useState(hoyISO());
  const [turnoSeleccionado, setTurnoSeleccionado] = useState(null);
  const [motivoConsulta, setMotivoConsulta] = useState('');
  const [reprogramando, setReprogramando] = useState(turnoAReprogramar);
  const [reprogramadoOk, setReprogramadoOk] = useState(false);
  const [avisoReprogramacion, setAvisoReprogramacion] = useState(null);

  useEffect(() => {
    dispatch(fetchEspecialidades());
    dispatch(fetchMercadoPagoEstado());
  }, [dispatch]);

  useEffect(() => {
    if (!perfil) dispatch(fetchMiPerfil());
  }, [dispatch, perfil]);

  useEffect(() => {
    // Al salir de este wizard (a "Mis turnos", u otra especialidad desde el
    // nav) limpiamos el resultado de la última reserva — si no, volver acá
    // (por ejemplo al hacer clic en "Reprogramar") mostraba de nuevo la
    // pantalla de éxito de la reserva anterior en vez del wizard.
    return () => { dispatch(limpiarReserva()); };
  }, [dispatch]);

  useEffect(() => {
    if (especialidadId) {
      dispatch(limpiarDisponibles());
      dispatch(fetchMedicos(especialidadId));
    }
    // No reseteamos medicoId acá: si venimos de "Reprogramar" necesitamos que el
    // médico preseleccionado sobreviva a este efecto disparado por el mismo cambio inicial de especialidad.
  }, [especialidadId, dispatch]);

  useEffect(() => {
    if (reprogramando && medicos.some((m) => m.id === reprogramando.medicoId) && !medicoId) {
      setMedicoId(String(reprogramando.medicoId));
    }
  }, [medicos, reprogramando, medicoId]);

  useEffect(() => {
    if (medicoId && fecha) {
      dispatch(fetchTurnosDisponibles({ medicoId, fecha }));
    }
  }, [medicoId, fecha, dispatch]);

  useEffect(() => {
    if (medicoId && fecha && status !== 'loading' && disponibles.length === 0) {
      dispatch(fetchMiAnotacionListaEspera({ medicoId, fecha }));
      dispatch(fetchProximaDisponibilidad({ medicoId, desde: fecha }));
    }
  }, [medicoId, fecha, status, disponibles.length, dispatch]);

  function handleAnotarseListaEspera() {
    setAnotandoListaEspera(true);
    dispatch(anotarseListaEspera({ medicoId, fecha })).unwrap().finally(() => setAnotandoListaEspera(false));
  }

  function handleEspecialidadChange(e) {
    const nuevaEspecialidadId = e.target.value;
    const especialidad = especialidades.find((esp) => String(esp.id) === nuevaEspecialidadId);
    if (especialidad?.esKinesiologia) {
      navigate('/turnos/kinesiologia');
      return;
    }
    setEspecialidadId(nuevaEspecialidadId);
    setMedicoId('');
    setTurnoSeleccionado(null);
    // reprogramando se mantiene: aunque cambie de especialidad o médico, el
    // turno viejo se sigue liberando al confirmar uno nuevo (ver "Cancelar
    // reprogramación" para el paciente que se arrepiente).
  }

  function handleMedicoChange(e) {
    setMedicoId(e.target.value);
    setTurnoSeleccionado(null);
  }

  async function handleReservar() {
    try {
      await dispatch(reservarTurno({ turnoId: turnoSeleccionado.id, motivoConsulta })).unwrap();
      if (reprogramando) {
        setReprogramadoOk(true);
        try {
          await dispatch(cancelarTurno(reprogramando.id)).unwrap();
        } catch {
          setAvisoReprogramacion(
            'El nuevo turno se reservó, pero no pudimos liberar el turno anterior automáticamente. Cancelalo desde "Mis turnos" o contactate con el centro.'
          );
        }
      }
    } catch {
      // El error ya queda en el estado de Redux (turnos.error) y se muestra abajo.
    }
  }

  async function handlePagarOnline() {
    try {
      const { initPoint } = await dispatch(pagarTurnoOnline(reservaExitosa.id)).unwrap();
      window.location.href = initPoint;
    } catch {
      // El error ya queda en turnos.pagoOnlineError y se muestra abajo.
    }
  }

  if (reservaExitosa) {
    const medicoReservado = medicos.find((m) => m.id === reservaExitosa.medicoId);
    const mostrarAvisoCopagoIoma = perfil?.obraSocial === 'IOMA' && medicoReservado?.cobraCopagoIoma;
    // Mismo criterio que el backend (MercadoPagoService.crearPreferencia): nada de kinesiología
    // acá porque ese flujo nunca pasa por este wizard (se redirige aparte, ver handleEspecialidadChange),
    // así que alcanza con que el médico tenga tarifa de consulta cargada.
    const puedePagarOnline = mercadoPagoHabilitado && !!medicoReservado?.tarifaConsulta;
    return (
      <div className="card" style={{ maxWidth: 480, margin: '0 auto', textAlign: 'center' }}>
        <div className="alert alert-success" role="status">
          {reprogramadoOk ? '¡Turno reprogramado con éxito!' : '¡Turno reservado con éxito!'}
        </div>
        {avisoReprogramacion && <div className="alert alert-error" role="alert" style={{ marginTop: 10 }}>{avisoReprogramacion}</div>}
        {mostrarAvisoCopagoIoma && (
          <div className="alert" style={{ marginTop: 10, background: 'var(--teal-tint)', color: 'var(--teal-dark)', border: '1px solid var(--teal)', textAlign: 'left' }}>
            Este médico cobra un copago a pacientes con IOMA. Por favor escribinos por WhatsApp para terminar de confirmar el turno.
            <div style={{ marginTop: 10 }}>
              <a
                className="btn btn-primary"
                href={`https://wa.me/${WHATSAPP_CONSULTORIO.numero}?text=${encodeURIComponent(WHATSAPP_CONSULTORIO.texto)}`}
                target="_blank"
                rel="noopener noreferrer"
              >
                Escribinos por WhatsApp
              </a>
            </div>
          </div>
        )}
        <h2 style={{ fontSize: 22 }}>{reservaExitosa.especialidadNombre} con {reservaExitosa.medicoNombre}</h2>
        <p style={{ marginTop: 10, color: 'var(--muted)' }}>
          {reservaExitosa.fecha} de {reservaExitosa.horaInicio.slice(0, 5)} a {reservaExitosa.horaFin.slice(0, 5)}
        </p>
        <div style={{ display: 'flex', justifyContent: 'center', marginTop: 20 }}>
          <AgregarCalendario turno={reservaExitosa} />
        </div>

        {puedePagarOnline && (
          <div style={{ marginTop: 24, paddingTop: 20, borderTop: '1px solid var(--line)' }}>
            <p style={{ fontSize: 14, color: 'var(--muted)', marginBottom: 12 }}>
              También podés pagar la consulta ahora online. Si preferís, se puede pagar en el consultorio igual.
            </p>
            {pagoOnlineError && <div className="alert alert-error" role="alert" style={{ marginBottom: 10 }}>{pagoOnlineError}</div>}
            <button className="btn btn-primary btn-block" onClick={handlePagarOnline} disabled={pagoOnlineCargando}>
              {pagoOnlineCargando ? 'Iniciando pago…' : 'Pagar ahora con Mercado Pago'}
            </button>
          </div>
        )}

        <div style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 24 }}>
          <button className="btn btn-outline" onClick={() => {
            dispatch(limpiarReserva());
            setTurnoSeleccionado(null);
            setReprogramando(null);
            setReprogramadoOk(false);
            setAvisoReprogramacion(null);
          }}>
            Sacar otro turno
          </button>
          <button className="btn btn-primary" onClick={() => navigate('/turnos/mis-turnos')}>Ver mis turnos</button>
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 640, margin: '0 auto' }}>
      <span className="eyebrow">{reprogramando ? 'Reprogramar turno' : 'Sacar turno'}</span>
      <h1 style={{ marginTop: 12, fontSize: 30 }}>Elegí especialidad, médico y horario</h1>

      {reprogramando && (
        <div className="alert" style={{ marginTop: 16, background: 'var(--teal-tint)', color: 'var(--teal-dark)', border: '1px solid var(--teal)' }}>
          Estás reprogramando tu turno de {reprogramando.especialidadNombre} con {reprogramando.medicoNombre} del{' '}
          {reprogramando.fecha} a las {reprogramando.horaInicio.slice(0, 5)}. Elegí un nuevo horario abajo — el turno
          anterior se libera automáticamente cuando confirmes el nuevo.{' '}
          <button
            type="button"
            onClick={() => setReprogramando(null)}
            style={{ background: 'none', border: 'none', padding: 0, color: 'inherit', textDecoration: 'underline', cursor: 'pointer', fontSize: 'inherit' }}
          >
            Cancelar reprogramación
          </button>
        </div>
      )}

      <div className="card" style={{ marginTop: 28 }}>
        {error && <div className="alert alert-error" role="alert">{error}</div>}

        <div className="field">
          <label htmlFor="especialidad">Especialidad</label>
          <select id="especialidad" value={especialidadId} onChange={handleEspecialidadChange}>
            <option value="">Seleccioná una especialidad</option>
            {especialidades.map((e) => <option key={e.id} value={e.id}>{e.nombre}</option>)}
          </select>
        </div>

        {especialidadId && (
          <div className="field">
            <label htmlFor="medico">Médico</label>
            <select id="medico" value={medicoId} onChange={handleMedicoChange}>
              <option value="">Seleccioná un médico</option>
              {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
            </select>
            {medicos.length === 0 && <p style={{ fontSize: 13, color: 'var(--muted)', marginTop: 4 }}>No hay médicos cargados para esta especialidad todavía.</p>}
          </div>
        )}

        {medicoId && (
          <div className="field">
            <label htmlFor="fecha">Fecha</label>
            <input id="fecha" type="date" min={hoyISO()} value={fecha} onChange={(e) => { setFecha(e.target.value); setTurnoSeleccionado(null); }} />
          </div>
        )}

        {medicoId && (
          <div className="field">
            <label id="horariosDisponiblesLabel">Horarios disponibles</label>
            {status === 'loading' && <p style={{ color: 'var(--muted)' }}>Buscando horarios…</p>}
            {status !== 'loading' && disponibles.length === 0 && (
              <>
                <p style={{ color: 'var(--muted)' }}>No hay horarios disponibles para esa fecha. Probá con otro día.</p>
                {proximaDisponibilidad.length > 0 && (
                  <div style={{ marginTop: 8, marginBottom: 12 }}>
                    <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 6 }}>Próxima disponibilidad:</p>
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                      {proximaDisponibilidad.map((f) => (
                        <button
                          type="button"
                          key={f}
                          className="btn btn-outline"
                          style={{ padding: '8px 16px', fontSize: 13.5 }}
                          onClick={() => { setFecha(f); setTurnoSeleccionado(null); }}
                        >
                          {formatFechaCorta(f)}
                        </button>
                      ))}
                    </div>
                  </div>
                )}
                {miAnotacionListaEspera ? (
                  <p style={{ fontSize: 13.5, color: 'var(--teal-dark)' }}>
                    Ya estás anotado en la lista de espera para este día — te avisamos por si se libera un turno.{' '}
                    <button
                      type="button"
                      onClick={() => dispatch(darseDeBajaListaEspera(miAnotacionListaEspera.id))}
                      style={{ background: 'none', border: 'none', padding: 0, color: 'inherit', textDecoration: 'underline', cursor: 'pointer', fontSize: 'inherit' }}
                    >
                      Darme de baja
                    </button>
                  </p>
                ) : (
                  <button type="button" className="btn btn-outline" style={{ marginTop: 8, padding: '8px 16px', fontSize: 13.5 }} onClick={handleAnotarseListaEspera} disabled={anotandoListaEspera}>
                    {anotandoListaEspera ? 'Anotando…' : 'Avisame si se libera un turno'}
                  </button>
                )}
              </>
            )}
            <div role="group" aria-labelledby="horariosDisponiblesLabel" style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
              {disponibles.map((t) => (
                <button
                  type="button"
                  key={t.id}
                  onClick={() => setTurnoSeleccionado(t)}
                  className="btn"
                  aria-pressed={turnoSeleccionado?.id === t.id}
                  style={{
                    border: turnoSeleccionado?.id === t.id ? '1.5px solid var(--teal)' : '1.5px solid var(--line)',
                    background: turnoSeleccionado?.id === t.id ? 'var(--teal-tint)' : 'var(--white)',
                    color: 'var(--ink)',
                    padding: '9px 16px',
                    fontSize: 14,
                  }}
                >
                  {t.horaInicio.slice(0, 5)}
                </button>
              ))}
            </div>
          </div>
        )}

        {turnoSeleccionado && (
          <>
            <div className="field">
              <label htmlFor="motivo">Motivo de consulta (opcional)</label>
              <textarea id="motivo" rows={3} value={motivoConsulta} onChange={(e) => setMotivoConsulta(e.target.value)} />
            </div>
            <button className="btn btn-primary btn-block" onClick={handleReservar} disabled={status === 'loading'}>
              {status === 'loading' ? 'Guardando…' : reprogramando ? `Confirmar nuevo horario de ${turnoSeleccionado.horaInicio.slice(0, 5)}` : `Confirmar turno de ${turnoSeleccionado.horaInicio.slice(0, 5)}`}
            </button>
          </>
        )}
      </div>
    </div>
  );
}
