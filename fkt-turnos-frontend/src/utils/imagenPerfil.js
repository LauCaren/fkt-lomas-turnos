// Recorte + compresión de una foto del lado del cliente — evita subir los 4-8MB que saca una
// cámara de celular moderna cuando la imagen nunca se muestra a full resolución. La usan tanto
// la foto de perfil circular (SelectorFotoPerfil.jsx, siempre cuadrada) como la foto "tipo
// carnet" de un médico para la web pública (AdminMedicosTab.jsx, retrato — ver
// `recortarYComprimir` más abajo, que por eso toma ancho/alto en vez de un solo lado fijo).
const LADO_DESTINO_DEFAULT = 640;
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
// react-easy-crop en `onCropComplete`. `anchoDestino`/`altoDestino` definen el tamaño final del
// archivo — por default un cuadrado (para el avatar circular); pasar los dos explícitos para un
// recorte rectangular (ej. una foto tipo carnet, con CropeadorFoto en aspect !== 1).
export function recortarYComprimir(imagen, areaRecorte, anchoDestino = LADO_DESTINO_DEFAULT, altoDestino = LADO_DESTINO_DEFAULT) {
  const canvas = document.createElement('canvas');
  canvas.width = anchoDestino;
  canvas.height = altoDestino;
  const ctx = canvas.getContext('2d');
  ctx.drawImage(
    imagen,
    areaRecorte.x, areaRecorte.y, areaRecorte.width, areaRecorte.height,
    0, 0, anchoDestino, altoDestino
  );
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) { reject(new Error('No se pudo procesar la imagen')); return; }
      resolve(new File([blob], 'foto.jpg', { type: 'image/jpeg' }));
    }, 'image/jpeg', CALIDAD_JPEG);
  });
}
