// Ocho avatares genéricos fijos — la clave (`key`) es lo único que se guarda en el backend
// (Usuario.avatarGenerico); el color y el dibujo viven acá, nunca como archivo de imagen, para
// no tener que subir/servir nada por un avatar que de entrada es "no tengo una foto real".
// Deben coincidir uno a uno con UsuarioFotoService.AVATARES_VALIDOS en el backend.
export const AVATARES_GENERICOS = [
  { key: 'avatar_1', color: '#239A96' },
  { key: 'avatar_2', color: '#1C3240' },
  { key: 'avatar_3', color: '#C77B3D' },
  { key: 'avatar_4', color: '#7B5EA7' },
  { key: 'avatar_5', color: '#3D6FB4' },
  { key: 'avatar_6', color: '#B4523D' },
  { key: 'avatar_7', color: '#4C8C5B' },
  { key: 'avatar_8', color: '#8A6D3B' },
];

export function colorDeAvatar(key) {
  return AVATARES_GENERICOS.find((a) => a.key === key)?.color || '#6D8089';
}
