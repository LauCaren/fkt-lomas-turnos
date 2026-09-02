export default function StatCard({ etiqueta, valor, destacado, onClick }) {
  const Comp = onClick ? 'button' : 'div';
  return (
    <Comp
      type={onClick ? 'button' : undefined}
      onClick={onClick}
      className={`stat-card${onClick ? ' stat-card-btn' : ''}`}
    >
      <div className="stat-card-label">{etiqueta}</div>
      <div className={`stat-card-value${destacado ? ' destacado' : ''}`}>{valor}</div>
    </Comp>
  );
}
