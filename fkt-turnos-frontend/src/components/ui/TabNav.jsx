// Envoltorio de la sección activa de un panel (admin, secretaría, médico, contador): el key fuerza
// a React a remontar el contenido al cambiar de sección, así corre la animación de entrada
// (.page-fade). La navegación en sí la hace SidebarNav.
export function TabPanel({ tabKey, children }) {
  return <div key={tabKey} className="page-fade">{children}</div>;
}
