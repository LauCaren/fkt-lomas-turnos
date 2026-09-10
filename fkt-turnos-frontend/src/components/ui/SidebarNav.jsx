/**
 * Reemplazo de TabNav para paneles con muchas secciones (admin, secretaría, contador, médico):
 * agrupa los items por tema y les pone un ícono, en vez de una fila de texto plano que se
 * envuelve en varias líneas. En pantallas angostas se degrada a una barra horizontal con
 * scroll, sin JS extra (ver .sidebar-nav en index.css).
 *
 * `groups`: [{ label?: string, items: [{ id, label, icon: ComponenteDeIcono }] }]
 */
export default function SidebarNav({ groups, active, onChange }) {
  return (
    <nav className="sidebar-nav" aria-label="Secciones del panel">
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
              </button>
            );
          })}
        </div>
      ))}
    </nav>
  );
}
