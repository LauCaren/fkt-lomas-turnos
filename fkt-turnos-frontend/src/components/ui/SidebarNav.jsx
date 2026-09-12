import { useLayoutEffect, useRef, useState } from 'react';

/**
 * Reemplazo de TabNav para paneles con muchas secciones (admin, secretaría, contador, médico):
 * agrupa los items por tema y les pone un ícono, en vez de una fila de texto plano que se
 * envuelve en varias líneas. En pantallas angostas se degrada a una barra horizontal con
 * scroll, sin JS extra (ver .sidebar-nav en index.css).
 *
 * `groups`: [{ label?: string, items: [{ id, label, icon: ComponenteDeIcono }] }]
 */
export default function SidebarNav({ groups, active, onChange }) {
  const navRef = useRef(null);
  // El resaltado ya no lo pinta cada botón por separado (activar uno, desactivar otro a la vez)
  // — un solo bloque desliza de una posición a la siguiente, que es lo que de verdad se lee como
  // movimiento dirigido en vez de dos recoloreos simultáneos. Solo aplica en el layout de
  // columna (desktop): en la barra horizontal de mobile el activo se resuelve con CSS puro
  // (ver .sidebar-btn.active en el media query), sin medir nada.
  const [indicador, setIndicador] = useState(null);

  useLayoutEffect(() => {
    function medir() {
      const nav = navRef.current;
      if (!nav) return;
      const btnActivo = nav.querySelector('.sidebar-btn.active');
      if (!btnActivo) {
        setIndicador(null);
        return;
      }
      const rectNav = nav.getBoundingClientRect();
      const rectBtn = btnActivo.getBoundingClientRect();
      setIndicador({
        top: rectBtn.top - rectNav.top,
        left: rectBtn.left - rectNav.left,
        width: rectBtn.width,
        height: rectBtn.height,
      });
    }
    medir();
    window.addEventListener('resize', medir);
    return () => window.removeEventListener('resize', medir);
  }, [active, groups]);

  return (
    <nav className="sidebar-nav" aria-label="Secciones del panel" ref={navRef}>
      {indicador && (
        <span
          className="sidebar-indicator"
          aria-hidden="true"
          style={{ transform: `translate(${indicador.left}px, ${indicador.top}px)`, width: indicador.width, height: indicador.height }}
        />
      )}
      {groups.map((group, i) => (
        <div className="sidebar-group" key={group.label || i}>
          {group.label && <div className="sidebar-group-label">{group.label}</div>}
          {group.items.map((item) => {
            const Icon = item.icon;
            return (
              <button
                key={item.id}
                type="button"
                role="tab"
                aria-selected={active === item.id}
                className={`sidebar-btn${active === item.id ? ' active' : ''}`}
                onClick={() => onChange(item.id)}
              >
                {Icon && <Icon aria-hidden="true" />}
                {item.label}
                {item.badge > 0 && <span className="sidebar-badge">{item.badge}</span>}
              </button>
            );
          })}
        </div>
      ))}
    </nav>
  );
}
