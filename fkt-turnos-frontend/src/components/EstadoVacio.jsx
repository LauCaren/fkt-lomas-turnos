// Estado vacío reutilizable para listas/tablas sin datos — un mensaje claro
// en vez de una tabla en blanco, que es lo primero que ve un usuario nuevo
// (médico/contador, que no son usuarios técnicos) al entrar por primera vez.
export default function EstadoVacio({ titulo, subtitulo }) {
  return (
    <div style={{ textAlign: 'center', padding: '40px 20px', border: '1px dashed var(--line)', borderRadius: 12 }}>
      <div style={{ fontSize: 16, fontWeight: 600, marginBottom: subtitulo ? 4 : 0 }}>{titulo}</div>
      {subtitulo && <div style={{ fontSize: 13.5, color: 'var(--muted)' }}>{subtitulo}</div>}
    </div>
  );
}
