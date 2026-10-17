// Encabezado de los paneles internos: el saludo manda y abajo va el contexto que de verdad
// se usa al arrancar el día (qué panel es y qué fecha es hoy), en vez de un rótulo suelto
// arriba del título.
const fmtHoy = new Intl.DateTimeFormat('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });

export default function PanelSaludo({ nombre, panel }) {
  return (
    <div className="page-head panel-saludo">
      <h1>Hola, {nombre}</h1>
      <p>{panel} · <span className="panel-saludo-fecha">{fmtHoy.format(new Date())}</span></p>
    </div>
  );
}
