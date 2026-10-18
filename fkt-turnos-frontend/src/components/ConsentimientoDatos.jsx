import { SITE_URL } from '../config';

// Casilla de consentimiento del alta de pacientes (registro con email y con Google). Un solo
// componente para que el texto legal sea idéntico en los dos caminos.
//
// Por qué este texto: la Ley 25.326 pide consentimiento libre, expreso e informado (art. 5), y
// los datos de salud son sensibles (art. 7) — por eso se nombran explícitamente "datos de salud"
// y la finalidad, en vez de un "acepto los términos" genérico. El backend guarda que se aceptó y
// por qué canal (Usuario.aceptaPoliticaPrivacidad / origenConsentimiento).
export default function ConsentimientoDatos({ id, checked, onChange, name }) {
  return (
    <label className="check" htmlFor={id}>
      <input id={id} name={name} type="checkbox" required checked={checked} onChange={onChange} />
      <span>
        Leí y acepto la{' '}
        <a href={`${SITE_URL}/politica-de-privacidad.html`} target="_blank" rel="noopener noreferrer" className="link-fuerte">política de privacidad</a>
        {' '}y los{' '}
        <a href={`${SITE_URL}/terminos-y-condiciones.html`} target="_blank" rel="noopener noreferrer" className="link-fuerte">términos y condiciones</a>,
        y doy mi consentimiento para que FKT Lomas trate mis datos personales y de salud para gestionar mis turnos y mi atención.
      </span>
    </label>
  );
}
