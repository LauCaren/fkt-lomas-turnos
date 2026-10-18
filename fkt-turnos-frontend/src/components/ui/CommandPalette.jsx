import { useEffect, useMemo, useRef, useState } from 'react';
import { Search, User } from 'lucide-react';
import Modal from './Modal';

// Aplana los grupos {label?, items:[{id,label,icon}]} que cada panel ya arma para su
// propio SidebarNav (GRUPOS_NAV) en una sola lista buscable — reusa exactamente esa
// estructura en vez de inventar una nueva.
function aplanarItems(groups) {
  const items = [];
  groups.forEach((g) => {
    g.items.forEach((item) => items.push({ ...item, grupo: g.label, tipo: 'nav' }));
  });
  return items;
}

/**
 * `comandos`: acciones ejecutables además de navegar (ej. "Nuevo turno"), shape
 * `{id, label, icon, run}`. `onBuscarPacientes`: si viene, además de secciones/comandos
 * busca pacientes en vivo por nombre/DNI/teléfono (reusa el mismo endpoint que ya usa
 * AdminBusquedaPacienteTab) y `onSeleccionarPaciente(paciente)` decide qué hacer con el
 * resultado elegido — ninguno de los dos es obligatorio (ej. contador no tiene acceso
 * clínico a pacientes, así que no los recibe).
 */
export default function CommandPalette({ groups, onNavigate, comandos = [], onBuscarPacientes, onSeleccionarPaciente }) {
  const [abierto, setAbierto] = useState(false);

  useEffect(() => {
    function handleKeyDown(e) {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault();
        setAbierto((v) => !v);
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  return (
    <>
      <button
        type="button"
        className="command-palette-trigger"
        onClick={() => setAbierto(true)}
        title="Buscar (Ctrl+K)"
      >
        <Search aria-hidden="true" />
        <span>Buscar{onBuscarPacientes ? ' paciente o sección' : ' sección'}…</span>
        <kbd>Ctrl K</kbd>
      </button>

      {/* El contenido solo existe mientras la paleta está abierta: cada vez que se abre arranca
          con la búsqueda vacía, sin tener que resetear estado a mano. */}
      {abierto && (
        <PaletaAbierta
          groups={groups}
          comandos={comandos}
          onNavigate={onNavigate}
          onBuscarPacientes={onBuscarPacientes}
          onSeleccionarPaciente={onSeleccionarPaciente}
          onCerrar={() => setAbierto(false)}
        />
      )}
    </>
  );
}

// Lista vacía estable (la misma referencia en cada render) para no recalcular los memos de abajo.
const SIN_PACIENTES = [];

function PaletaAbierta({ groups, comandos, onNavigate, onBuscarPacientes, onSeleccionarPaciente, onCerrar }) {
  const [query, setQuery] = useState('');
  const [indiceActivo, setIndiceActivo] = useState(0);
  // Resultado de la última búsqueda de pacientes, con el texto que la originó: solo se muestra
  // si todavía coincide con lo escrito (así no hace falta limpiarlo a mano al borrar).
  const [busqueda, setBusqueda] = useState({ q: '', pacientes: [], buscando: false });
  const inputRef = useRef(null);

  const qPacientes = query.trim();
  const buscaPacientes = Boolean(onBuscarPacientes) && qPacientes.length >= 2;
  const pacientes = buscaPacientes && busqueda.q === qPacientes ? busqueda.pacientes : SIN_PACIENTES;
  const buscandoPacientes = buscaPacientes && busqueda.buscando;

  const itemsNav = useMemo(() => aplanarItems(groups), [groups]);
  const itemsComando = useMemo(() => comandos.map((c) => ({ ...c, tipo: 'comando' })), [comandos]);

  const resultadosLocales = useMemo(() => {
    const q = query.trim().toLowerCase();
    const todos = [...itemsComando, ...itemsNav];
    if (!q) return todos;
    return todos.filter((item) => item.label.toLowerCase().includes(q) || (item.grupo || '').toLowerCase().includes(q));
  }, [itemsNav, itemsComando, query]);

  const resultadosPacientes = useMemo(() => pacientes.map((p) => ({
    id: `paciente-${p.id}`,
    tipo: 'paciente',
    label: p.nombreCompleto,
    grupo: `DNI ${p.dni} · ${p.telefono}`,
    paciente: p,
  })), [pacientes]);

  const resultados = useMemo(() => [...resultadosLocales, ...resultadosPacientes], [resultadosLocales, resultadosPacientes]);

  // Búsqueda en vivo de pacientes: debounced, y solo con texto suficiente para no pegarle
  // a la API en cada tecla ni con una query vacía.
  useEffect(() => {
    if (!buscaPacientes) return undefined;
    const timer = setTimeout(() => {
      setBusqueda((b) => ({ ...b, buscando: true }));
      onBuscarPacientes(qPacientes)
        .then((data) => setBusqueda({ q: qPacientes, pacientes: data || [], buscando: false }))
        .catch(() => setBusqueda({ q: qPacientes, pacientes: [], buscando: false }));
    }, 300);
    return () => clearTimeout(timer);
  }, [qPacientes, buscaPacientes, onBuscarPacientes]);

  function cambiarQuery(valor) {
    setQuery(valor);
    setIndiceActivo(0);
  }

  function seleccionar(item) {
    if (item.tipo === 'comando') {
      item.run();
    } else if (item.tipo === 'paciente') {
      onSeleccionarPaciente?.(item.paciente);
    } else {
      onNavigate(item.id);
    }
    onCerrar();
  }

  function handleInputKeyDown(e) {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setIndiceActivo((i) => Math.min(i + 1, resultados.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setIndiceActivo((i) => Math.max(i - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (resultados[indiceActivo]) seleccionar(resultados[indiceActivo]);
    }
  }

  return (
    <Modal onCerrar={onCerrar} labelledBy="paletaComandosTitulo" className="command-palette" initialFocusRef={inputRef}>
      <h2 id="paletaComandosTitulo" className="sr-only">Buscar una sección, un paciente o una acción</h2>
      <div className="command-palette-input">
        <Search aria-hidden="true" />
        <input
          ref={inputRef}
          type="text"
          placeholder={onBuscarPacientes ? 'Paciente por nombre o DNI, o una sección…' : 'Agenda, Caja, Reseñas…'}
          value={query}
          onChange={(e) => cambiarQuery(e.target.value)}
          onKeyDown={handleInputKeyDown}
        />
      </div>
      <div className="command-palette-results">
        {resultados.length === 0 && !buscandoPacientes && <p className="command-palette-empty">Sin resultados</p>}
        {buscandoPacientes && resultadosPacientes.length === 0 && <p className="command-palette-empty">Buscando pacientes…</p>}
        {resultados.map((item, i) => {
          const Icon = item.tipo === 'paciente' ? User : item.icon;
          return (
            <button
              key={item.id}
              type="button"
              className={`command-palette-item${i === indiceActivo ? ' active' : ''}`}
              onClick={() => seleccionar(item)}
              onMouseEnter={() => setIndiceActivo(i)}
            >
              {Icon && <Icon aria-hidden="true" />}
              <span className="command-palette-item-label">{item.label}</span>
              {item.grupo && <span className="command-palette-item-group">{item.grupo}</span>}
            </button>
          );
        })}
      </div>
    </Modal>
  );
}
