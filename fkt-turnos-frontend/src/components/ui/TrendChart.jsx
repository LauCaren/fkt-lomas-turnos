import { formatFechaCorta } from '../../utils/formato';

// Gráfico de barras simple (sin librería) para mostrar una serie diaria — ej. turnos
// reservados o ingresos de los últimos N días. `datos` es una lista de objetos con un
// campo `fecha` ("YYYY-MM-DD"); `valorDe` extrae el número a graficar de cada uno.
export default function TrendChart({ titulo, datos, valorDe, formatValor, color = 'var(--teal)' }) {
  if (!datos || datos.length === 0) return null;

  const valores = datos.map(valorDe);
  const max = Math.max(...valores, 1);
  const anchoBarra = 100 / datos.length;
  const alturaMaxima = 34;

  return (
    <div className="card" style={{ marginBottom: 20 }}>
      <h3 style={{ fontSize: 15, marginBottom: 14, color: 'var(--muted)', fontWeight: 600 }}>{titulo}</h3>
      <svg viewBox="0 0 100 40" preserveAspectRatio="none" style={{ width: '100%', height: 110, display: 'block' }}>
        {datos.map((d, i) => {
          const valor = valorDe(d);
          const altura = max > 0 ? (valor / max) * alturaMaxima : 0;
          return (
            <rect
              key={d.fecha}
              className="trend-bar"
              style={{ animationDelay: `${Math.min(i * 15, 450)}ms` }}
              x={i * anchoBarra + anchoBarra * 0.15}
              y={36 - altura}
              width={anchoBarra * 0.7}
              height={Math.max(altura, 0.6)}
              fill={color}
            >
              <title>{formatFechaCorta(d.fecha)}: {formatValor ? formatValor(valor) : valor}</title>
            </rect>
          );
        })}
      </svg>
      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 11, color: 'var(--muted)', marginTop: 4 }}>
        <span>{formatFechaCorta(datos[0].fecha)}</span>
        <span>{formatFechaCorta(datos[datos.length - 1].fecha)}</span>
      </div>
    </div>
  );
}
