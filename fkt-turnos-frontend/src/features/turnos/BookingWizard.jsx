import { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation, useNavigate } from 'react-router-dom';
import { CalendarCheck, ChevronRight } from 'lucide-react';
import {
  fetchEspecialidades, fetchMedicos, fetchTurnosDisponibles, reservarTurno, cancelarTurno, limpiarReserva, limpiarDisponibles,
  fetchMiAnotacionListaEspera, anotarseListaEspera, darseDeBajaListaEspera, fetchProximaDisponibilidad,
  fetchMercadoPagoEstado, pagarTurnoOnline,
} from './turnosSlice';
import { fetchMiPerfil } from '../cuenta/cuentaSlice';
import AgregarCalendario from '../../components/AgregarCalendario';
import Goniometro from '../../components/ui/Goniometro';
import { hoyISO, formatFechaCorta, formatMoneda } from '../../utils/formato';

const WHATSAPP_CONSULTORIO = { numero: '5491123515402', texto: 'Hola, quiero confirmar mi turno con copago IOMA.' };

const PASOS = ['Especialidad', 'Profesional', 'Día', 'Horario'];
// Cada paso es una parada del goniómetro, centrada en su cuarto del arco.
const PARADAS = PASOS.map((_, i) => (i + 0.5) / PASOS.length);
const DIAS_A_MOSTRAR = 14;

// Fechas "YYYY-MM-DD" armadas a mano en hora local (nunca new Date("YYYY-MM-DD"), que se
// interpreta como UTC y en Argentina corre el día hacia atrás).
function aFecha(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}
function aISO(fecha) {
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, '0')}-${String(fecha.getDate()).padStart(2, '0')}`;
}
function proximosDias(desdeISO, cantidad) {
  const base = aFecha(desdeISO);
  return Array.from({ length: cantidad }, (_, i) => aISO(new Date(base.getFullYear(), base.getMonth(), base.getDate() + i)));
}
const fmtSemanaCorta = new Intl.DateTimeFormat('es-AR', { weekday: 'short' });
const fmtMesCorto = new Intl.DateTimeFormat('es-AR', { month: 'short' });
const fmtLargo = new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });
function fechaLarga(iso) {
  const texto = fmtLargo.format(aFecha(iso));
  return texto.charAt(0).toUpperCase() + texto.slice(1);
}
function sinPunto(texto) {
  return texto.replace('.', '');
}

// Agrupa los turnos libres por hora para la escala graduada: cada hora es un renglón y cada
// turno cae en la columna de su cuarto de hora (0-3), así los :30 quedan siempre alineados.
function escalaPorHora(turnos) {
  const horas = new Map();
  for (const t of turnos) {
    const [h, m] = t.horaInicio.split(':').map(Number);
    if (!horas.has(h)) horas.set(h, [null, null, null, null]);
    const columna = Math.min(3, Math.floor(m / 15));
    const fila = horas.get(h);
    // Dos turnos en el mismo cuarto (agendas de 10 min) no deberían pisarse: el segundo va
    // al próximo hueco libre de la fila.
    let c = columna;
    while (c < 4 && fila[c]) c++;
    if (c < 4) fila[c] = t;
  }
  return [...horas.entries()].sort((a, b) => a[0] - b[0]);
}

function PasosReserva({ paso, valores, onIr }) {
  return (
    <ol className="reserva-pasos" aria-label="Pasos para sacar el turno">
      {PASOS.map((nombre, i) => {
        const hecho = i < paso;
        const contenido = (
          <>
            <span className="reserva-paso-nombre">{nombre}</span>
            <span className="reserva-paso-valor">{hecho ? valores[i] : ' '}</span>
          </>
        );
        return (
          <li
            key={nombre}
            className={`reserva-paso-item${hecho ? ' reserva-paso-hecho' : ''}`}
            aria-current={i === paso ? 'step' : undefined}
          >
            {hecho ? (
              <button type="button" className="reserva-paso-btn" onClick={() => onIr(i)} aria-label={`Cambiar ${nombre.toLowerCase()}: ${valores[i]}`}>
                {contenido}
              </button>
            ) : (
              <span className="reserva-paso-btn">{contenido}</span>
            )}
          </li>
        );
      })}
    </ol>
  );
}

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
  // El slice no marca la carga de especialidades/médicos: sin esto, mientras llegaban se
  // veía un instante el mensaje de "no hay" en vez del esqueleto.
  const [especialidadesCargadas, setEspecialidadesCargadas] = useState(false);
  const [medicosCargadosDe, setMedicosCargadosDe] = useState(null);

  const turnoAReprogramar = location.state?.reprogramar || null;

  const [especialidadId, setEspecialidadId] = useState(turnoAReprogramar?.especialidadId ? String(turnoAReprogramar.especialidadId) : '');
  // Al reprogramar se arranca con la misma especialidad y el mismo profesional del turno original.
  const [medicoId, setMedicoId] = useState(turnoAReprogramar?.medicoId ? String(turnoAReprogramar.medicoId) : '');
  const [fecha, setFecha] = useState(hoyISO());
  const [turnoSeleccionado, setTurnoSeleccionado] = useState(null);
  const [motivoConsulta, setMotivoConsulta] = useState('');
  const [reprogramando, setReprogramando] = useState(turnoAReprogramar);
  const [reprogramadoOk, setReprogramadoOk] = useState(false);
  const [avisoReprogramacion, setAvisoReprogramacion] = useState(null);
  // Al reprogramar, especialidad y médico ya vienen del turno viejo: se arranca eligiendo el día.
  const [paso, setPaso] = useState(turnoAReprogramar ? 2 : 0);
  const [direccion, setDireccion] = useState('adelante');
  // Al cambiar de paso el foco va al título nuevo (teclado y lector de pantalla siguen el
  // flujo); en la carga inicial no, para no robarle el foco a nadie.
  const tituloRef = useRef(null);
  const enfocarTitulo = useRef(false);

  useEffect(() => {
    if (!enfocarTitulo.current) return;
    // Volver arriba muestra la aguja moviéndose al paso nuevo; el foco va al título sin
    // otro salto de scroll.
    const sinMovimiento = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    window.scrollTo({ top: 0, behavior: sinMovimiento ? 'auto' : 'smooth' });
    tituloRef.current?.focus({ preventScroll: true });
  }, [paso]);

  useEffect(() => {
    dispatch(fetchEspecialidades()).finally(() => setEspecialidadesCargadas(true));
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
      dispatch(fetchMedicos(especialidadId)).finally(() => setMedicosCargadosDe(especialidadId));
    }
    // No reseteamos medicoId acá: si venimos de "Reprogramar" necesitamos que el
    // médico preseleccionado sobreviva a este efecto disparado por el mismo cambio inicial de especialidad.
  }, [especialidadId, dispatch]);

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

  // Con el turno confirmado se vuelve arriba: la aguja completando el arco y el "¡Turno
  // reservado!" son lo primero que tiene que ver el paciente, no el final de la escala.
  useEffect(() => {
    if (reservaExitosa) window.scrollTo({ top: 0 });
  }, [reservaExitosa]);

  function irAPaso(nuevo) {
    enfocarTitulo.current = true;
    setDireccion(nuevo < paso ? 'atras' : 'adelante');
    setPaso(nuevo);
  }

  function handleAnotarseListaEspera() {
    setAnotandoListaEspera(true);
    dispatch(anotarseListaEspera({ medicoId, fecha })).unwrap().finally(() => setAnotandoListaEspera(false));
  }

  function elegirEspecialidad(especialidad) {
    if (especialidad.esKinesiologia) {
      navigate('/turnos/kinesiologia');
      return;
    }
    const id = String(especialidad.id);
    if (id !== especialidadId) {
      setEspecialidadId(id);
      setMedicoId('');
      setTurnoSeleccionado(null);
    }
    // reprogramando se mantiene: aunque cambie de especialidad o médico, el
    // turno viejo se sigue liberando al confirmar uno nuevo (ver "Cancelar
    // reprogramación" para el paciente que se arrepiente).
    irAPaso(1);
  }

  function elegirMedico(medico) {
    const id = String(medico.id);
    if (id !== medicoId) {
      setMedicoId(id);
      setTurnoSeleccionado(null);
    }
    irAPaso(2);
  }

  function elegirFecha(nuevaFecha) {
    if (!nuevaFecha) return;
    if (nuevaFecha !== fecha) {
      setFecha(nuevaFecha);
      setTurnoSeleccionado(null);
    }
    irAPaso(3);
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

  function sacarOtroTurno() {
    dispatch(limpiarReserva());
    setTurnoSeleccionado(null);
    setReprogramando(null);
    setReprogramadoOk(false);
    setAvisoReprogramacion(null);
    setMotivoConsulta('');
    irAPaso(0);
  }

  if (reservaExitosa) {
    const medicoReservado = medicos.find((m) => m.id === reservaExitosa.medicoId);
    const mostrarAvisoCopagoIoma = perfil?.obraSocial === 'IOMA' && medicoReservado?.cobraCopagoIoma;
    // Mismo criterio que el backend (MercadoPagoService.crearPreferencia): nada de kinesiología
    // acá porque ese flujo nunca pasa por este wizard (se redirige aparte, ver elegirEspecialidad),
    // así que alcanza con que el médico tenga tarifa de consulta cargada.
    const puedePagarOnline = mercadoPagoHabilitado && !!medicoReservado?.tarifaConsulta;
    return (
      <div className="confirmado">
        <div className="confirmado-medidor">
          <Goniometro progreso={1} paradas={PARADAS} />
        </div>
        <p className="confirmado-estado" role="status">
          <CalendarCheck size={18} aria-hidden="true" />
          {reprogramadoOk ? '¡Turno reprogramado con éxito!' : '¡Turno reservado con éxito!'}
        </p>
        <h1>{reservaExitosa.especialidadNombre} con {reservaExitosa.medicoNombre}</h1>
        <div className="confirmado-lectura">
          <span className="confirmado-dia">{fechaLarga(reservaExitosa.fecha)}</span>
          <span className="confirmado-hora">
            {reservaExitosa.horaInicio.slice(0, 5)} <span>a {reservaExitosa.horaFin.slice(0, 5)}</span>
          </span>
        </div>

        <div className="card confirmado-hoja">
          {avisoReprogramacion && <div className="alert alert-error" role="alert">{avisoReprogramacion}</div>}
          {mostrarAvisoCopagoIoma && (
            <div className="reserva-aviso" style={{ marginTop: 0 }}>
              Este médico cobra un copago a pacientes con IOMA. Por favor escribinos por WhatsApp para terminar de confirmar el turno.
              <div style={{ marginTop: 12 }}>
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
          <div>
            <AgregarCalendario turno={reservaExitosa} conTitulo />
          </div>

          {puedePagarOnline && (
            <div style={{ paddingTop: 16, borderTop: '1px solid var(--line)' }}>
              <p style={{ fontSize: 15, color: 'var(--muted)', marginBottom: 12 }}>
                También podés pagar la consulta ahora online. Si preferís, se puede pagar en el consultorio igual —
                el pago online es opcional y no es un requisito para conservar tu turno. Si necesitás cancelar
                después de pagar, escribinos por WhatsApp o llamanos para coordinarlo.
              </p>
              {pagoOnlineError && <div className="alert alert-error" role="alert">{pagoOnlineError}</div>}
              <button className="btn btn-primary btn-block" onClick={handlePagarOnline} disabled={pagoOnlineCargando}>
                {pagoOnlineCargando ? 'Iniciando pago…' : 'Pagar ahora con Mercado Pago'}
              </button>
            </div>
          )}
        </div>

        <div className="confirmado-acciones">
          <button className="btn btn-outline" onClick={sacarOtroTurno}>Sacar otro turno</button>
          <button className="btn btn-primary" onClick={() => navigate('/turnos/mis-turnos')}>Ver mis turnos</button>
        </div>
      </div>
    );
  }

  const especialidad = especialidades.find((e) => String(e.id) === especialidadId);
  const medico = medicos.find((m) => String(m.id) === medicoId);
  const valoresPasos = [
    especialidad?.nombre || reprogramando?.especialidadNombre || '',
    medico?.nombreCompleto || '',
    fecha ? `${sinPunto(fmtSemanaCorta.format(aFecha(fecha)))} ${formatFechaCorta(fecha)}` : '',
    turnoSeleccionado ? turnoSeleccionado.horaInicio.slice(0, 5) : '',
  ];
  const cargando = status === 'loading';
  const cargandoMedicos = medicosCargadosDe !== especialidadId;
  // La aguja avanza a la parada del paso actual, y un tramo más cuando ya hay un horario
  // elegido: el último movimiento antes de confirmar.
  const progreso = PARADAS[paso] + (paso === 3 && turnoSeleccionado ? 0.08 : 0);

  return (
    <div className="reserva">
      <div className="reserva-medidor">
        <Goniometro progreso={progreso} paradas={PARADAS} />
      </div>
      <PasosReserva paso={paso} valores={valoresPasos} onIr={irAPaso} />

      {reprogramando && (
        <div className="reserva-aviso">
          Estás reprogramando tu turno de {reprogramando.especialidadNombre} con {reprogramando.medicoNombre} del{' '}
          {formatFechaCorta(reprogramando.fecha)} a las {reprogramando.horaInicio.slice(0, 5)}. El turno anterior se libera
          automáticamente cuando confirmes el nuevo.{' '}
          <button type="button" onClick={() => setReprogramando(null)} className="link-btn">
            Cancelar reprogramación
          </button>
        </div>
      )}

      <section key={paso} className={`reserva-etapa reserva-etapa-${direccion}`}>
        {error && <div className="alert alert-error" role="alert">{error}</div>}

        {paso === 0 && (
          <>
            <div className="reserva-etapa-cabecera">
              <div>
                <h1 ref={tituloRef} tabIndex={-1}>¿Qué especialidad necesitás?</h1>
                <p>Elegí una para ver los profesionales que atienden.</p>
              </div>
            </div>
            <div className="reserva-contenido opciones">
              {!especialidadesCargadas && especialidades.length === 0 && [0, 1, 2, 3].map((i) => <span key={i} className="skeleton opcion-esqueleto" />)}
              {especialidadesCargadas && especialidades.length === 0 && (
                <p className="field-hint">Todavía no hay especialidades disponibles para reservar online. Llamanos y te damos un turno.</p>
              )}
              {especialidades.map((e) => (
                <button
                  type="button"
                  key={e.id}
                  className="opcion"
                  aria-pressed={String(e.id) === especialidadId}
                  onClick={() => elegirEspecialidad(e)}
                >
                  <span className="opcion-texto">
                    <span className="opcion-titulo">{e.nombre}</span>
                    {e.esKinesiologia && <span className="opcion-detalle">Plan de sesiones, se reserva aparte</span>}
                  </span>
                  <ChevronRight className="opcion-flecha" size={22} aria-hidden="true" />
                </button>
              ))}
            </div>
          </>
        )}

        {paso === 1 && (
          <>
            <div className="reserva-etapa-cabecera">
              <div>
                <h1 ref={tituloRef} tabIndex={-1}>¿Con quién te querés atender?</h1>
                <p>{especialidad?.nombre || reprogramando?.especialidadNombre}</p>
              </div>
            </div>
            <div className="reserva-contenido opciones">
              {cargandoMedicos && [0, 1, 2].map((i) => <span key={i} className="skeleton opcion-esqueleto" />)}
              {!cargandoMedicos && medicos.length === 0 && (
                <p className="field-hint">No hay profesionales cargados para esta especialidad todavía.</p>
              )}
              {!cargandoMedicos && medicos.map((m) => (
                <button
                  type="button"
                  key={m.id}
                  className="opcion"
                  aria-pressed={String(m.id) === medicoId}
                  onClick={() => elegirMedico(m)}
                >
                  <span className="opcion-texto">
                    <span className="opcion-titulo">{m.nombreCompleto}</span>
                    {m.tarifaConsulta ? (
                      <span className="opcion-detalle">
                        Consulta {formatMoneda(m.tarifaConsulta)}{m.cobraCopagoIoma && ' · copago IOMA'}
                      </span>
                    ) : null}
                  </span>
                  <ChevronRight className="opcion-flecha" size={22} aria-hidden="true" />
                </button>
              ))}
            </div>
          </>
        )}

        {paso === 2 && (
          <>
            <div className="reserva-etapa-cabecera">
              <div>
                <h1 ref={tituloRef} tabIndex={-1}>¿Qué día te queda bien?</h1>
                <p>{medico?.nombreCompleto || reprogramando?.medicoNombre}</p>
              </div>
            </div>
            <div className="reserva-contenido">
              <div className="dias" role="group" aria-label="Próximos días">
                {proximosDias(hoyISO(), DIAS_A_MOSTRAR).map((d, i) => {
                  const fechaD = aFecha(d);
                  return (
                    <button
                      type="button"
                      key={d}
                      className={`dia${i === 0 ? ' dia-hoy' : ''}`}
                      aria-pressed={d === fecha}
                      aria-label={fechaLarga(d)}
                      onClick={() => elegirFecha(d)}
                    >
                      <span className="dia-semana">{i === 0 ? 'Hoy' : sinPunto(fmtSemanaCorta.format(fechaD))}</span>
                      <span className="dia-numero">{fechaD.getDate()}</span>
                      <span className="dia-mes">{sinPunto(fmtMesCorto.format(fechaD))}</span>
                    </button>
                  );
                })}
              </div>
              <div className="dias-otra">
                <div className="field">
                  <label htmlFor="fecha">¿Más adelante? Elegí otra fecha</label>
                  <input id="fecha" type="date" min={hoyISO()} value={fecha} onChange={(e) => elegirFecha(e.target.value)} />
                </div>
              </div>
            </div>
          </>
        )}

        {paso === 3 && (
          <>
            <div className="reserva-etapa-cabecera">
              <div>
                <h1 ref={tituloRef} tabIndex={-1}>Elegí el horario</h1>
                <p>{fechaLarga(fecha)} · {medico?.nombreCompleto || reprogramando?.medicoNombre}</p>
              </div>
              <button type="button" className="btn btn-ghost" onClick={() => irAPaso(2)}>Cambiar día</button>
            </div>

            <div className="reserva-contenido">
              {cargando && (
                <div className="escala" aria-hidden="true">
                  {[0, 1, 2].map((i) => (
                    <div key={i} className="escala-hora">
                      <span className="skeleton" style={{ height: 22, width: 36 }} />
                      {[0, 1, 2, 3].map((c) => <span key={c} className="skeleton horario-esqueleto" />)}
                    </div>
                  ))}
                </div>
              )}

              {!cargando && disponibles.length === 0 && (
                <div className="reserva-sin-turnos">
                  <h2>No quedan turnos libres ese día</h2>
                  <p>Probá con otro día{proximaDisponibilidad.length > 0 ? ' — estos son los próximos con lugar:' : '.'}</p>
                  {proximaDisponibilidad.length > 0 && (
                    <div className="reserva-proximas">
                      {proximaDisponibilidad.map((f) => (
                        <button type="button" key={f} className="btn btn-outline" onClick={() => elegirFecha(f)}>
                          {sinPunto(fmtSemanaCorta.format(aFecha(f)))} {formatFechaCorta(f)}
                        </button>
                      ))}
                    </div>
                  )}
                  <div className="reserva-lista-espera">
                    {miAnotacionListaEspera ? (
                      <p style={{ color: 'var(--teal-dark)' }}>
                        Ya estás en la lista de espera de este día — te avisamos si se libera un turno.{' '}
                        <button
                          type="button"
                          onClick={() => dispatch(darseDeBajaListaEspera(miAnotacionListaEspera.id))}
                          className="link-btn link-fuerte"
                          style={{ background: 'none', border: 'none', padding: 0, fontSize: 'inherit' }}
                        >
                          Darme de baja
                        </button>
                      </p>
                    ) : (
                      <button type="button" className="btn btn-outline" onClick={handleAnotarseListaEspera} disabled={anotandoListaEspera}>
                        {anotandoListaEspera ? 'Anotando…' : 'Avisame si se libera un turno'}
                      </button>
                    )}
                  </div>
                </div>
              )}

              {!cargando && disponibles.length > 0 && (
                <div className="escala" role="group" aria-label={`Horarios libres del ${fechaLarga(fecha)}`}>
                  {escalaPorHora(disponibles).map(([hora, columnas]) => (
                    <div key={hora} className="escala-hora">
                      <span className="escala-etiqueta" aria-hidden="true">{String(hora).padStart(2, '0')}<span>h</span></span>
                      {columnas.map((t, c) => (t ? (
                        <button
                          type="button"
                          key={t.id}
                          className="horario"
                          aria-pressed={turnoSeleccionado?.id === t.id}
                          onClick={() => setTurnoSeleccionado(t)}
                        >
                          {t.horaInicio.slice(0, 5)}
                        </button>
                      ) : (
                        <span key={`vacio-${c}`} className="escala-vacio" aria-hidden="true" />
                      )))}
                    </div>
                  ))}
                </div>
              )}

              {turnoSeleccionado && (
                <>
                  <div className="field" style={{ marginTop: 22, marginBottom: 0 }}>
                    <label htmlFor="motivo">Motivo de consulta (opcional)</label>
                    <textarea id="motivo" rows={2} value={motivoConsulta} onChange={(e) => setMotivoConsulta(e.target.value)}
                      placeholder="Por ejemplo: dolor de rodilla desde hace dos semanas" />
                  </div>
                  <div className="lectura" key={turnoSeleccionado.id}>
                    <span className="lectura-hora">{turnoSeleccionado.horaInicio.slice(0, 5)}</span>
                    <span className="lectura-detalle">
                      <strong>{fechaLarga(fecha)}</strong>
                      <span>{especialidad?.nombre || reprogramando?.especialidadNombre} · {medico?.nombreCompleto || reprogramando?.medicoNombre}</span>
                    </span>
                    <button className="btn btn-primary btn-block btn-lg" onClick={handleReservar} disabled={cargando}>
                      {cargando && <span className="spinner" aria-hidden="true" />}
                      {cargando ? 'Guardando…' : reprogramando
                        ? `Confirmar nuevo horario de ${turnoSeleccionado.horaInicio.slice(0, 5)}`
                        : `Confirmar turno de ${turnoSeleccionado.horaInicio.slice(0, 5)}`}
                    </button>
                  </div>
                </>
              )}
            </div>
          </>
        )}
      </section>
    </div>
  );
}
