import { OBRAS_SOCIALES, OBRA_SOCIAL_PARTICULAR, OBRA_SOCIAL_OTRA } from '../constants/obrasSociales';

const CONOCIDAS = new Set([OBRA_SOCIAL_PARTICULAR, ...OBRAS_SOCIALES, OBRA_SOCIAL_OTRA]);

// El <select> de obra social que comparten el registro, "Mi cuenta", el alta con Google y las
// altas telefónicas de la secretaría (agenda y kinesiología). Antes estaba copiado en cinco lugares.
//
// Si la persona tiene guardada una obra social que no está en la lista (una que se dejó de
// atender, o un dato viejo), se agrega como opción más: sin esto el <select> la mostraba vacía y,
// al guardar el perfil por otro motivo, se perdía el dato.
export default function SelectorObraSocial({ id, name, value, onChange, placeholder = 'Seleccioná una obra social' }) {
  const valorFueraDeLista = value && !CONOCIDAS.has(value);
  return (
    <select id={id} name={name} required value={value} onChange={onChange}>
      <option value="">{placeholder}</option>
      {valorFueraDeLista && <option value={value}>{value}</option>}
      <option value={OBRA_SOCIAL_PARTICULAR}>{OBRA_SOCIAL_PARTICULAR}</option>
      {OBRAS_SOCIALES.map((os) => <option key={os} value={os}>{os}</option>)}
      <option value={OBRA_SOCIAL_OTRA}>{OBRA_SOCIAL_OTRA}</option>
    </select>
  );
}
