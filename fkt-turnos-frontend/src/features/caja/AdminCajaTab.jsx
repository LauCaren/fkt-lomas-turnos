import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  fetchResumenCaja, fetchIngresos, registrarIngreso, anularIngreso,
  fetchRetiros, registrarRetiro, anularRetiro,
  fetchCategoriasGasto, crearCategoriaGasto, fetchGastos, registrarGasto, anularGasto,
  limpiarMensajesCaja,
} from './cajaSlice';
import { fetchMedicosAdmin } from '../admin/adminSlice';
import { verArchivo } from '../../api/descargas';
import { formatMoneda, formatFechaHora } from '../../utils/formato';
import EstadoVacio from '../../components/EstadoVacio';
import { useAhora } from '../../hooks/useAhora';

function verComprobante(gastoId) {
  return verArchivo(`/api/admin/caja/gastos/${gastoId}/comprobante`);
}

const METODOS_PAGO = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'MERCADOPAGO', label: 'MercadoPago' },
];

function AnularBoton({ onAnular }) {
  const [motivo, setMotivo] = useState('');
  const [abierto, setAbierto] = useState(false);

  if (!abierto) {
    return <button className="btn btn-outline btn-sm" onClick={() => setAbierto(true)}>Anular</button>;
  }
  return (
    <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
      <input placeholder="Motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} style={{ fontSize: 13.5, padding: '4px 8px', width: 160 }} />
      <button
        className="btn btn-outline btn-sm"
        disabled={!motivo.trim()}
        onClick={() => { onAnular(motivo.trim()); setAbierto(false); setMotivo(''); }}
      >
        Confirmar
      </button>
    </div>
  );
}

function ResumenTab() {
  const dispatch = useDispatch();
  const resumen = useSelector((state) => state.caja.resumen);

  useEffect(() => { dispatch(fetchResumenCaja()); }, [dispatch]);

  if (!resumen) {
    return (
      <div style={{ display: 'flex', gap: 16, flexWrap: 'wrap' }}>
        <div className="skeleton" style={{ height: 66, minWidth: 180, flex: '1 1 180px' }} />
        <div className="skeleton" style={{ height: 66, minWidth: 180, flex: '1 1 180px' }} />
        <div className="skeleton" style={{ height: 66, minWidth: 180, flex: '1 1 180px' }} />
        <div className="skeleton" style={{ height: 66, minWidth: 180, flex: '1 1 180px' }} />
      </div>
    );
  }

  return (
    <div>
      <div className="stagger" style={{ display: 'flex', gap: 16, flexWrap: 'wrap', marginBottom: 24 }}>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 14, color: 'var(--muted)' }}>Caja física</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.cajaFisica)}</div>
        </div>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 14, color: 'var(--muted)' }}>Ingresos totales</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.ingresosTotal)}</div>
        </div>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 14, color: 'var(--muted)' }}>Retiros totales</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.retirosTotal)}</div>
        </div>
        <div className="card" style={{ minWidth: 180 }}>
          <div style={{ fontSize: 14, color: 'var(--muted)' }}>Gastos totales</div>
          <div style={{ fontSize: 22, fontWeight: 700 }}>{formatMoneda(resumen.gastosTotal)}</div>
        </div>
      </div>

      <h3 style={{ fontSize: 16, marginBottom: 12 }}>Saldo a favor por médico</h3>
      {resumen.saldosPorMedico.length === 0 && <EstadoVacio titulo="Todavía no hay movimientos" subtitulo="Cuando se registre un ingreso o retiro, va a aparecer acá el saldo de cada médico." />}
      <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {resumen.saldosPorMedico.map((s) => (
          <div key={s.medicoId} className="card" style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 18px' }}>
            <span>{s.medicoNombre}</span>
            <span style={{ display: 'flex', gap: 16, fontSize: 14.5 }}>
              <span style={{ color: 'var(--muted)' }}>Cobrado {formatMoneda(s.ingresosTotal)}</span>
              <span style={{ color: 'var(--muted)' }}>Retirado {formatMoneda(s.retirosTotal)}</span>
              <strong>Saldo {formatMoneda(s.saldo)}</strong>
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function IngresosTab() {
  const dispatch = useDispatch();
  const { ingresos } = useSelector((state) => state.caja);
  const medicos = useSelector((state) => state.admin.medicos);
  const [form, setForm] = useState({ medicoId: '', turnoId: '', monto: '', metodoPago: 'EFECTIVO', fecha: '' });
  const ahora = useAhora();

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
    dispatch(fetchIngresos());
  }, [dispatch]);

  function handleMedicoChange(medicoId) {
    const medico = medicos.find((m) => String(m.id) === medicoId);
    setForm({ ...form, medicoId, monto: medico?.tarifaConsulta ? String(medico.tarifaConsulta) : form.monto });
  }

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(registrarIngreso({
      medicoId: Number(form.medicoId),
      turnoId: form.turnoId ? Number(form.turnoId) : null,
      monto: Number(form.monto),
      metodoPago: form.metodoPago,
      fecha: form.fecha || null,
    })).unwrap().then(() => setForm({ medicoId: '', turnoId: '', monto: '', metodoPago: 'EFECTIVO', fecha: '' })).catch(() => {});
  }

  return (
    <div>
      <div className="card" style={{ marginBottom: 20 }}>
        <h3 style={{ fontSize: 16, marginBottom: 12 }}>Registrar cobro</h3>
        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 200px' }}>
            <label htmlFor="cajaCobroMedico">Médico</label>
            <select id="cajaCobroMedico" required value={form.medicoId} onChange={(e) => handleMedicoChange(e.target.value)}>
              <option value="">Seleccioná un médico</option>
              {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
            <label htmlFor="cajaCobroMonto">Monto</label>
            <input id="cajaCobroMonto" type="number" min="0.01" step="0.01" required value={form.monto} onChange={(e) => setForm({ ...form, monto: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 160px' }}>
            <label htmlFor="cajaCobroMetodo">Método de pago</label>
            <select id="cajaCobroMetodo" value={form.metodoPago} onChange={(e) => setForm({ ...form, metodoPago: e.target.value })}>
              {METODOS_PAGO.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
            <label htmlFor="cajaCobroFecha">Fecha (opcional)</label>
            <input id="cajaCobroFecha" type="date" value={form.fecha} onChange={(e) => setForm({ ...form, fecha: e.target.value })} />
          </div>
          <button type="submit" className="btn btn-primary">Registrar</button>
        </form>
      </div>

      {ingresos.length > 0 && (
        <button type="button" className="btn btn-outline" style={{ marginBottom: 12 }} onClick={() => window.print()}>Imprimir listado</button>
      )}
      <div id="ingresos-imprimir">
        <div className="ficha-print-header">
          <strong>FKT Lomas</strong> · Listado de cobros · Impreso el {ahora.toLocaleDateString('es-AR')}
        </div>
        <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          {ingresos.map((i) => (
            <div key={i.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 18px', opacity: i.anulado ? 0.55 : 1 }}>
              <div>
                <div style={{ fontWeight: 600 }}>{i.medicoNombre} · {formatMoneda(i.monto)}</div>
                <div style={{ fontSize: 13.5, color: 'var(--muted)' }}>
                  {i.fecha} · {METODOS_PAGO.find((m) => m.value === i.metodoPago)?.label} · registrado por {i.registradoPorNombre}
                  {i.anulado && ` · ANULADO (${i.motivoAnulacion})`}
                </div>
              </div>
              {!i.anulado && <AnularBoton onAnular={(motivo) => dispatch(anularIngreso({ id: i.id, motivo }))} />}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

function RetirosTab() {
  const dispatch = useDispatch();
  const { retiros } = useSelector((state) => state.caja);
  const medicos = useSelector((state) => state.admin.medicos);
  const [form, setForm] = useState({ medicoId: '', monto: '' });

  useEffect(() => {
    dispatch(fetchMedicosAdmin());
    dispatch(fetchRetiros());
  }, [dispatch]);

  function handleSubmit(e) {
    e.preventDefault();
    dispatch(registrarRetiro({ medicoId: Number(form.medicoId), monto: Number(form.monto) })).unwrap()
      .then(() => setForm({ medicoId: '', monto: '' })).catch(() => {});
  }

  return (
    <div>
      <div className="card" style={{ marginBottom: 20 }}>
        <h3 style={{ fontSize: 16, marginBottom: 12 }}>Registrar retiro</h3>
        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 200px' }}>
            <label htmlFor="cajaRetiroMedico">Médico</label>
            <select id="cajaRetiroMedico" required value={form.medicoId} onChange={(e) => setForm({ ...form, medicoId: e.target.value })}>
              <option value="">Seleccioná un médico</option>
              {medicos.map((m) => <option key={m.id} value={m.id}>{m.nombreCompleto}</option>)}
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
            <label htmlFor="cajaRetiroMonto">Monto entregado</label>
            <input id="cajaRetiroMonto" type="number" min="0.01" step="0.01" required value={form.monto} onChange={(e) => setForm({ ...form, monto: e.target.value })} />
          </div>
          <button type="submit" className="btn btn-primary">Registrar retiro</button>
        </form>
      </div>

      <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {retiros.map((r) => (
          <div key={r.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 18px', opacity: r.anulado ? 0.55 : 1 }}>
            <div>
              <div style={{ fontWeight: 600 }}>{r.medicoNombre} · {formatMoneda(r.monto)}</div>
              <div style={{ fontSize: 13.5, color: 'var(--muted)' }}>
                {formatFechaHora(r.fechaHora)} · entregado por {r.entregadoPorNombre}
                {r.anulado && ` · ANULADO (${r.motivoAnulacion})`}
              </div>
            </div>
            {!r.anulado && <AnularBoton onAnular={(motivo) => dispatch(anularRetiro({ id: r.id, motivo }))} />}
          </div>
        ))}
      </div>
    </div>
  );
}

function GastosTab() {
  const dispatch = useDispatch();
  const { categorias, gastos } = useSelector((state) => state.caja);
  const [form, setForm] = useState({ categoriaId: '', monto: '', fecha: '', descripcion: '' });
  const [comprobante, setComprobante] = useState(null);
  const [nuevaCategoria, setNuevaCategoria] = useState('');

  useEffect(() => {
    dispatch(fetchCategoriasGasto());
    dispatch(fetchGastos());
  }, [dispatch]);

  function handleSubmit(e) {
    e.preventDefault();
    const formData = new FormData();
    formData.append('categoriaId', form.categoriaId);
    formData.append('monto', form.monto);
    if (form.fecha) formData.append('fecha', form.fecha);
    if (form.descripcion) formData.append('descripcion', form.descripcion);
    if (comprobante) formData.append('comprobante', comprobante);
    dispatch(registrarGasto(formData)).unwrap().then(() => {
      setForm({ categoriaId: '', monto: '', fecha: '', descripcion: '' });
      setComprobante(null);
    }).catch(() => {});
  }

  function handleNuevaCategoria(e) {
    e.preventDefault();
    if (!nuevaCategoria.trim()) return;
    dispatch(crearCategoriaGasto(nuevaCategoria.trim())).unwrap().then(() => setNuevaCategoria('')).catch(() => {});
  }

  return (
    <div>
      <div className="card" style={{ marginBottom: 20 }}>
        <h3 style={{ fontSize: 16, marginBottom: 12 }}>Categorías</h3>
        <form onSubmit={handleNuevaCategoria} style={{ display: 'flex', gap: 8, marginBottom: 10 }}>
          <input placeholder="Nueva categoría" value={nuevaCategoria} onChange={(e) => setNuevaCategoria(e.target.value)} style={{ flex: 1, padding: '8px 12px', border: '1.5px solid var(--line)', borderRadius: 6 }} />
          <button type="submit" className="btn btn-outline">Agregar</button>
        </form>
        <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
          {categorias.map((c) => <span key={c.id} className="badge badge-cancelado">{c.nombre}</span>)}
        </div>
      </div>

      <div className="card" style={{ marginBottom: 20 }}>
        <h3 style={{ fontSize: 16, marginBottom: 12 }}>Registrar gasto</h3>
        <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 10, flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 180px' }}>
            <label htmlFor="cajaGastoCategoria">Categoría</label>
            <select id="cajaGastoCategoria" required value={form.categoriaId} onChange={(e) => setForm({ ...form, categoriaId: e.target.value })}>
              <option value="">Seleccioná una categoría</option>
              {categorias.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
            <label htmlFor="cajaGastoMonto">Monto</label>
            <input id="cajaGastoMonto" type="number" min="0.01" step="0.01" required value={form.monto} onChange={(e) => setForm({ ...form, monto: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '1 1 140px' }}>
            <label htmlFor="cajaGastoFecha">Fecha (opcional)</label>
            <input id="cajaGastoFecha" type="date" value={form.fecha} onChange={(e) => setForm({ ...form, fecha: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0, flex: '2 1 220px' }}>
            <label htmlFor="cajaGastoDescripcion">Descripción</label>
            <input id="cajaGastoDescripcion" value={form.descripcion} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} />
          </div>
          <div className="field" style={{ marginBottom: 0 }}>
            <label htmlFor="cajaGastoComprobante">Comprobante (opcional)</label>
            <input id="cajaGastoComprobante" type="file" accept="image/jpeg,image/png,application/pdf" onChange={(e) => setComprobante(e.target.files[0] || null)} style={{ fontSize: 13.5 }} />
          </div>
          <button type="submit" className="btn btn-primary">Registrar gasto</button>
        </form>
      </div>

      <div className="stagger" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {gastos.map((g) => (
          <div key={g.id} className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 18px', opacity: g.anulado ? 0.55 : 1 }}>
            <div>
              <div style={{ fontWeight: 600 }}>{g.categoriaNombre} · {formatMoneda(g.monto)}</div>
              <div style={{ fontSize: 13.5, color: 'var(--muted)' }}>
                {g.fecha} · {g.descripcion} · registrado por {g.registradoPorNombre}
                {g.anulado && ` · ANULADO (${g.motivoAnulacion})`}
              </div>
            </div>
            <div style={{ display: 'flex', gap: 8 }}>
              {g.tieneComprobante && (
                <button className="btn btn-outline btn-sm" onClick={() => verComprobante(g.id)}>Ver comprobante</button>
              )}
              {!g.anulado && <AnularBoton onAnular={(motivo) => dispatch(anularGasto({ id: g.id, motivo }))} />}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

const SUBTABS = [
  { id: 'resumen', label: 'Resumen' },
  { id: 'ingresos', label: 'Ingresos' },
  { id: 'retiros', label: 'Retiros' },
  { id: 'gastos', label: 'Gastos' },
];

export default function AdminCajaTab() {
  const dispatch = useDispatch();
  const { error, mensajeExito } = useSelector((state) => state.caja);
  const [subtab, setSubtab] = useState('resumen');

  return (
    <div>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 14 }}>{error}</div>}
      {mensajeExito && <div className="alert alert-success" role="status" style={{ marginBottom: 14 }}>{mensajeExito}</div>}

      <div style={{ display: 'flex', gap: 8, marginBottom: 20 }}>
        {SUBTABS.map((t) => (
          <button
            key={t.id}
            className={`btn ${subtab === t.id ? 'btn-primary' : 'btn-outline'}`}
            style={{ padding: '7px 16px', fontSize: 14.5 }}
            onClick={() => { setSubtab(t.id); dispatch(limpiarMensajesCaja()); }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {subtab === 'resumen' && <ResumenTab />}
      {subtab === 'ingresos' && <IngresosTab />}
      {subtab === 'retiros' && <RetirosTab />}
      {subtab === 'gastos' && <GastosTab />}
    </div>
  );
}
