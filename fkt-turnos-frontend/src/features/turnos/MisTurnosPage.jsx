import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { X } from 'lucide-react';
import { fetchMisTurnos, cancelarTurno } from './turnosSlice';
import AgregarCalendario from '../../components/AgregarCalendario';
import DejarResenaTurno from './DejarResenaTurno';
import EstadoVacio from '../../components/EstadoVacio';
import { useConfirm } from '../../components/ui/useConfirm';

// Mercado Pago vuelve acá con ?pago=exito|pendiente|fallo (ver MercadoPagoService.crearPreferenciaEnMercadoPago,
// back_urls) — el pago en sí se confirma por webhook, esto es solo feedback inmediato al paciente.
const AVISO_PAGO = {
  exito: { clase: 'alert-success', texto: 'Estamos confirmando tu pago — en unos minutos vas a verlo reflejado acá.' },
  pendiente: { clase: 'alert-warning', texto: 'Tu pago quedó pendiente de confirmación (por ejemplo, si pagaste con un medio que tarda en acreditar).' },
  fallo: { clase: 'alert-error', texto: 'El pago no se pudo completar. Podés reintentarlo o pagar en el consultorio.' },
};

// Para el paciente, "reservado" es la buena noticia (su turno está tomado): va en teal, no en
// el rojo que antes compartía con los errores.
const ESTADO = {
  DISPONIBLE: { clase: 'badge-disponible', texto: 'Disponible' },
  RESERVADO: { clase: 'badge-confirmado', texto: 'Reservado' },
  CANCELADO: { clase: 'badge-cancelado', texto: 'Cancelado' },
};

function aFecha(iso) {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}
const fmtSemana = new Intl.DateTimeFormat('es-AR', { weekday: 'long' });
const fmtMes = new Intl.DateTimeFormat('es-AR', { month: 'short' });
const fmtCorta = new Intl.DateTimeFormat('es-AR', { weekday: 'short', day: 'numeric', month: 'short' });

// En la lista de anteriores importa qué pasó con el turno, no que "estaba reservado".
function estadoDe(t) {
  if (t.estado === 'CANCELADO') return ESTADO.CANCELADO;
  if (t.estadoAsistencia === 'ATENDIDO') return { clase: 'badge-llego', texto: 'Atendido' };
  if (t.estadoAsistencia === 'AUSENTE') return { clase: 'badge-ausente', texto: 'No asististe' };
  return ESTADO[t.estado] || { clase: '', texto: t.estado };
}

function esFuturo(turno) {
  return new Date(`${turno.fecha}T${turno.horaInicio}`) >= new Date();
}

export default function MisTurnosPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const confirmar = useConfirm();
  const { misTurnos } = useSelector((state) => state.turnos);
  const [searchParams, setSearchParams] = useSearchParams();
  const avisoPago = AVISO_PAGO[searchParams.get('pago')];

  useEffect(() => {
    dispatch(fetchMisTurnos());
  }, [dispatch]);

  async function handleCancelar(turno) {
    const ok = await confirmar({
      titulo: '¿Cancelar este turno?',
      detalle: (
        <>
          <p style={{ marginBottom: 4 }}><strong>{turno.especialidadNombre}</strong> con {turno.medicoNombre}</p>
          <p style={{ color: 'var(--muted)' }}>{fmtCorta.format(aFecha(turno.fecha))} · {turno.horaInicio.slice(0, 5)} a {turno.horaFin.slice(0, 5)}</p>
          <p style={{ marginTop: 12 }}>El horario queda libre para otro paciente.</p>
        </>
      ),
      confirmarTexto: 'Sí, cancelar turno',
      peligroso: true,
    });
    if (ok) dispatch(cancelarTurno(turno.id));
  }

  function handleReprogramar(turno) {
    navigate('/turnos', { state: { reprogramar: turno } });
  }

  const proximos = misTurnos.filter((t) => t.estado === 'RESERVADO' && esFuturo(t));
  const anteriores = misTurnos.filter((t) => !(t.estado === 'RESERVADO' && esFuturo(t)));

  return (
    <div className="mis-turnos">
      <div className="page-head mis-turnos-cabecera">
        <div>
          <h1>Tus turnos</h1>
          <p>{proximos.length === 1 ? 'Tenés 1 turno próximo.' : proximos.length > 1 ? `Tenés ${proximos.length} turnos próximos.` : 'No tenés turnos próximos.'}</p>
        </div>
        <Link to="/turnos" className="btn btn-primary">Sacar turno</Link>
      </div>

      {avisoPago && (
        <div className={`alert ${avisoPago.clase} alert-cerrable`} role="status" style={{ marginTop: 20 }}>
          <span>{avisoPago.texto}</span>
          <button type="button" onClick={() => setSearchParams({}, { replace: true })} aria-label="Cerrar aviso" className="alert-cerrar">
            <X size={18} aria-hidden="true" />
          </button>
        </div>
      )}

      {misTurnos.length === 0 && (
        <div style={{ marginTop: 28 }}>
          <EstadoVacio
            titulo="Todavía no tenés turnos"
            subtitulo="Cuando saques un turno, lo vas a ver acá con todos sus datos."
            accion={<Link to="/turnos" className="btn btn-primary">Sacar un turno</Link>}
          />
        </div>
      )}

      {proximos.length > 0 && (
        <section className="mis-turnos-seccion" aria-labelledby="proximosTitulo">
          <h2 id="proximosTitulo" className="seccion-titulo">Próximos</h2>
          <ul className="stagger mis-turnos-lista">
            {proximos.map((t) => {
              const fecha = aFecha(t.fecha);
              return (
                <li key={t.id} className="turno-ficha">
                  <div className="turno-fecha" aria-hidden="true">
                    <span className="dia-semana">{fmtSemana.format(fecha)}</span>
                    <span className="dia-numero">{fecha.getDate()}</span>
                    <span className="dia-mes">{fmtMes.format(fecha).replace('.', '')}</span>
                  </div>
                  <div className="turno-cuerpo">
                    <p className="turno-hora">
                      {t.horaInicio.slice(0, 5)} <span>a {t.horaFin.slice(0, 5)}</span>
                      <span className="sr-only"> del {fmtSemana.format(fecha)} {fecha.getDate()}</span>
                    </p>
                    <p className="turno-quien"><strong>{t.especialidadNombre}</strong> con {t.medicoNombre}</p>
                    {t.motivoConsulta && <p className="turno-motivo">Motivo: {t.motivoConsulta}</p>}
                    <div className="turno-acciones">
                      <button className="btn btn-outline" onClick={() => handleReprogramar(t)}>Cambiar horario</button>
                      <button className="btn btn-ghost btn-peligro" onClick={() => handleCancelar(t)}>Cancelar</button>
                    </div>
                    <AgregarCalendario turno={t} />
                  </div>
                </li>
              );
            })}
          </ul>
        </section>
      )}

      {anteriores.length > 0 && (
        <section className="mis-turnos-seccion" aria-labelledby="anterioresTitulo">
          <h2 id="anterioresTitulo" className="seccion-titulo">Anteriores</h2>
          <ul className="mis-turnos-lista">
            {anteriores.map((t) => (
              <li key={t.id} className="turno-pasado">
                <div className="turno-pasado-fila">
                  <span className="turno-pasado-fecha">{fmtCorta.format(aFecha(t.fecha)).replace(/\./g, '')} · {t.horaInicio.slice(0, 5)}</span>
                  <span className="turno-pasado-quien"><strong>{t.especialidadNombre}</strong> con {t.medicoNombre}</span>
                  <span className={`badge ${estadoDe(t).clase}`}>{estadoDe(t).texto}</span>
                </div>
                {t.estadoAsistencia === 'ATENDIDO' && !t.tieneResena && <DejarResenaTurno turnoId={t.id} />}
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
