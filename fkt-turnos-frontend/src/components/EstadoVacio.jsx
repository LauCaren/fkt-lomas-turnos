// Estado vacío reutilizable para listas/tablas sin datos — un mensaje claro
// en vez de una tabla en blanco, que es lo primero que ve un usuario nuevo
// (médico/contador, que no son usuarios técnicos) al entrar por primera vez.
// `accion`, si viene, es un <Link>/<button> ya armado por quien llama (ej. "Sacar un turno") —
// este componente no conoce de routing, solo le da el mismo tratamiento visual que el resto.
export default function EstadoVacio({ titulo, subtitulo, accion }) {
  return (
    <div className="estado-vacio">
      <div className="estado-vacio-titulo">{titulo}</div>
      {subtitulo && <div className="estado-vacio-subtitulo">{subtitulo}</div>}
      {accion}
    </div>
  );
}
