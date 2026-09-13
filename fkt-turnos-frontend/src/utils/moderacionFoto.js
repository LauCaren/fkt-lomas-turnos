// Chequeos automáticos sobre la foto de perfil ANTES de subirla — corren enteramente en el
// navegador, nadie manda la imagen a un tercero para esto: los pesos de ambos modelos se sirven
// como estáticos propios desde /models (ver cargarFaceApi/cargarNsfwjs más abajo). Dos chequeos
// independientes:
//   - Detección de cara (face-api.js, modelo "tiny", ~190KB): si no encuentra una cara clara,
//     es una advertencia que el usuario puede pasar por alto (quizás es una foto de perfil con
//     mascota, o de espaldas a propósito).
//   - Contenido inapropiado (nsfwjs, MobileNetV2): si clasifica la imagen como Porn o Hentai con
//     alta confianza, se bloquea la subida directamente — no es algo que el usuario pueda pasar
//     por alto, porque esta foto la ve el resto del staff y otros pacientes.
// Ambos modelos se cargan una sola vez (lazy, recién cuando hace falta el primer chequeo) y se
// reutilizan en toda la sesión.

const UMBRAL_INAPROPIADA = 0.75;
const CATEGORIAS_BLOQUEADAS = new Set(['Porn', 'Hentai']);

// El inputSize default de TinyFaceDetectorOptions (416) está pensado para fotos grupales con
// caras chicas en cuadro — en una foto de perfil ya recortada a cuadrado, con una sola cara que
// ocupa la mayor parte del encuadre, ese tamaño funciona mal (probado a mano: falla en casos
// donde 224 detecta con >99% de confianza). 224 es el tamaño recomendado para este caso de uso.
const OPCIONES_DETECCION_CARA = { inputSize: 224, scoreThreshold: 0.4 };

let faceapiCargando = null;
let nsfwCargando = null;

async function cargarFaceApi() {
  if (!faceapiCargando) {
    faceapiCargando = import('face-api.js').then(async (faceapi) => {
      await faceapi.nets.tinyFaceDetector.loadFromUri('/models/face');
      return faceapi;
    });
  }
  return faceapiCargando;
}

async function cargarNsfwjs() {
  if (!nsfwCargando) {
    // 'nsfwjs' (el entry point normal) importa de arriba los TRES modelos que trae la
    // librería (MobileNetV2, MobileNetV2Mid, InceptionV3) para armar su registro interno,
    // aunque el código en runtime solo pida uno — el bundler no puede saber cuál hace falta y
    // termina empaquetando los tres (~30MB, la mayoría de InceptionV3, que ni usamos), lo que
    // además hace fallar el build por el límite de precache del service worker. 'nsfwjs/core'
    // es el mismo load() pero sin ese registro — pasándole una URL en vez de un nombre de
    // modelo usa tf.loadLayersModel directo, así que solo se bajan estos ~2.7MB propios,
    // servidos como estáticos desde /models/nsfw.
    nsfwCargando = import('nsfwjs/core').then((nsfwjs) => nsfwjs.load('/models/nsfw/model.json'));
  }
  return nsfwCargando;
}

// `archivo` es el File/Blob ya recortado y comprimido (ver imagenPerfil.js). Devuelve
// { caraDetectada, inapropiada } — si algún modelo falla en cargar o clasificar (sin conexión
// la primera vez, navegador sin WebGL, etc.) esa parte del chequeo se omite en vez de bloquear
// la subida: son una ayuda, no un requisito duro para poder tener foto de perfil.
export async function moderarFoto(archivo) {
  const url = URL.createObjectURL(archivo);
  try {
    const img = new Image();
    img.src = url;
    await img.decode();

    const [caraDetectada, inapropiada] = await Promise.all([
      cargarFaceApi()
        .then((faceapi) => faceapi.detectSingleFace(img, new faceapi.TinyFaceDetectorOptions(OPCIONES_DETECCION_CARA)))
        .then((deteccion) => !!deteccion)
        .catch(() => null),
      cargarNsfwjs()
        .then((modelo) => modelo.classify(img))
        .then((predicciones) => predicciones.some((p) => CATEGORIAS_BLOQUEADAS.has(p.className) && p.probability >= UMBRAL_INAPROPIADA))
        .catch(() => false),
    ]);

    return { caraDetectada, inapropiada };
  } finally {
    URL.revokeObjectURL(url);
  }
}
