import { Link } from 'react-router-dom';
import { SITE_URL } from '../config';

// Links legales obligatorios al pie de las pantallas de pacientes y públicas (login, registro,
// reserva). Mismos que el footer del sitio de marketing — ver docs/CUMPLIMIENTO-LEGAL.md:
//   - "Botón de arrepentimiento": Ley 24.240 art. 34 y Res. SCI 424/2020 (destacado, sin login).
//   - "Defensa de las y los Consumidores": Res. SCI 274/2021 (Ventanilla Única Federal).
//   - Política de privacidad y términos (Ley 25.326, art. 6: informar antes de recolectar datos).
// Los paneles internos (secretaría, médicos, admin, contador) no lo muestran: no son
// pantallas para consumidores y ahí cada píxel es para trabajar.
const URL_DEFENSA_CONSUMIDOR = 'https://www.argentina.gob.ar/produccion/defensadelconsumidor/formulario';

export default function PieLegal() {
  return (
    <footer className="pie-legal">
      <div className="wrap pie-legal-inner">
        <Link to="/arrepentimiento" className="pie-legal-arrepentimiento">Botón de arrepentimiento</Link>
        <nav aria-label="Información legal" className="pie-legal-links">
          <a href={`${SITE_URL}/politica-de-privacidad.html`} target="_blank" rel="noopener noreferrer">Política de privacidad</a>
          <a href={`${SITE_URL}/terminos-y-condiciones.html`} target="_blank" rel="noopener noreferrer">Términos y condiciones</a>
          <a href={URL_DEFENSA_CONSUMIDOR} target="_blank" rel="noopener noreferrer">Defensa de las y los Consumidores. Para reclamos ingrese aquí</a>
        </nav>
      </div>
    </footer>
  );
}
