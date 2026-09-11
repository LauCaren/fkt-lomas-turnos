import { useEffect, useMemo, useRef, useState } from 'react';
import { Search } from 'lucide-react';
import Modal from './Modal';

// Aplana los grupos {label?, items:[{id,label,icon}]} que cada panel ya arma para su
// propio SidebarNav (GRUPOS_NAV) en una sola lista buscable — reusa exactamente esa
// estructura en vez de inventar una nueva.
function aplanarItems(groups) {
  const items = [];
  groups.forEach((g) => {
    g.items.forEach((item) => items.push({ ...item, grupo: g.label }));
  });
  return items;
}

export default function CommandPalette({ groups, onNavigate }) {
  const [abierto, setAbierto] = useState(false);
  const [query, setQuery] = useState('');
  const [indiceActivo, setIndiceActivo] = useState(0);
  const inputRef = useRef(null);

  const items = useMemo(() => aplanarItems(groups), [groups]);
  const resultados = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return items;
    return items.filter((item) => item.label.toLowerCase().includes(q) || (item.grupo || '').toLowerCase().includes(q));
  }, [items, query]);

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

  useEffect(() => {
    if (abierto) {
      setQuery('');
      setIndiceActivo(0);
    }
  }, [abierto]);

  useEffect(() => { setIndiceActivo(0); }, [query]);

  function seleccionar(item) {
    onNavigate(item.id);
    setAbierto(false);
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
    <>
      <button
        type="button"
        className="command-palette-trigger"
        onClick={() => setAbierto(true)}
        title="Buscar una sección (Ctrl+K)"
      >
        <Search aria-hidden="true" />
        <span>Buscar sección…</span>
        <kbd>Ctrl K</kbd>
      </button>

      {abierto && (
        <Modal onCerrar={() => setAbierto(false)} labelledBy="paletaComandosTitulo" className="command-palette" initialFocusRef={inputRef}>
          <h2 id="paletaComandosTitulo" className="sr-only">Buscar una sección del panel</h2>
          <div className="command-palette-input">
            <Search aria-hidden="true" />
            <input
              ref={inputRef}
              type="text"
              placeholder="Agenda, Caja, Reseñas…"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleInputKeyDown}
            />
          </div>
          <div className="command-palette-results">
            {resultados.length === 0 && <p className="command-palette-empty">Sin resultados</p>}
            {resultados.map((item, i) => {
              const Icon = item.icon;
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
      )}
    </>
  );
}
