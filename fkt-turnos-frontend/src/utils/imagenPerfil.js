// Recorte + compresión de la foto de perfil, del lado del cliente — evita subir los 4-8MB que
// saca una cámara de celular moderna cuando el avatar nunca se muestra a más de ~100px. Se usa
// tanto para la foto elegida desde la galería como para la sacada con la cámara del dispositivo
// (ver CropeadorFoto.jsx), así las dos terminan pasando por el mismo recorte cuadrado.
const LADO_DESTINO = 640;
const CALIDAD_JPEG = 0.85;

export function cargarImagen(src) {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onload = () => resolve(img);
    img.onerror = () => reject(new Error('No se pudo leer la imagen'));
    img.src = src;
  });
}

// `areaRecorte` es el {x, y, width, height} en píxeles de la imagen original que devuelve
// react-easy-crop en `onCropComplete`. Siempre recorta a un cuadrado de LADO_DESTINO — el avatar
// se dibuja circular en toda la app, así que no tiene sentido guardar un rectángulo.
export function recortarYComprimir(imagen, areaRecorte) {
  const canvas = document.createElement('canvas');
  canvas.width = LADO_DESTINO;
  canvas.height = LADO_DESTINO;
  const ctx = canvas.getContext('2d');
  ctx.drawImage(
    imagen,
    areaRecorte.x, areaRecorte.y, areaRecorte.width, areaRecorte.height,
    0, 0, LADO_DESTINO, LADO_DESTINO
  );
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) { reject(new Error('No se pudo procesar la imagen')); return; }
      resolve(new File([blob], 'foto-perfil.jpg', { type: 'image/jpeg' }));
    }, 'image/jpeg', CALIDAD_JPEG);
  });
}
