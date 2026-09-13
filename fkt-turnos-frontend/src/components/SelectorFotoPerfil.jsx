import { useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import Modal from './ui/Modal';
import UsuarioAvatar from './UsuarioAvatar';
import CropeadorFoto from './CropeadorFoto';
import { useToast } from './ui/useToast';
import { AVATARES_GENERICOS, colorDeAvatar } from '../constants/avataresGenericos';
import AvatarGenericoIcono from './AvatarGenericoIcono';
import { cargarImagen, recortarYComprimir } from '../utils/imagenPerfil';
import { moderarFoto } from '../utils/moderacionFoto';
import { subirFotoPropia, elegirAvatarPropio, quitarFotoPropia, limpiarErrorPerfil } from '../features/perfil/perfilSlice';

const TIPOS_ACEPTADOS = ['image/jpeg', 'image/png'];
const TAMANO_MAXIMO_BYTES = 8 * 1024 * 1024;

/**
 * Modal de "mi foto de perfil" — subir desde la galería, sacarse una con la cámara ahí mismo,
 * o elegir uno de los 8 avatares genéricos. Se abre desde el avatar del header (Layout.jsx),
 * disponible para cualquier rol logueado: cada uno administra la suya, nunca la de otro.
 */
export default function SelectorFotoPerfil({ onCerrar }) {
  const dispatch = useDispatch();
  const toast = useToast();
  const { fotoInfo, fotoVersion, status, error } = useSelector((state) => state.perfil);
  const usuario = useSelector((state) => state.auth.usuario);
  const [vista, setVista] = useState('menu'); // 'menu' | 'camara' | 'recortar' | 'avatares'
  const inputArchivoRef = useRef(null);

  // Imagen sin recortar todavía (de la galería o recién sacada con la cámara) — object URL que
  // hay que revocar apenas deja de hacer falta, para no ir acumulando memoria.
  const [imagenParaRecortar, setImagenParaRecortar] = useState(null);
  const [procesandoRecorte, setProcesandoRecorte] = useState(false);
  const [errorModeracion, setErrorModeracion] = useState(null);
  // Foto ya recortada/comprimida que pasó el chequeo de contenido pero no encontró una cara
  // clara — queda "pausada" hasta que el usuario confirme que quiere usarla igual.
  const [fotoSinCaraConfirmar, setFotoSinCaraConfirmar] = useState(null);

  const guardando = status === 'loading';

  // Revoca el object URL de la imagen sin recortar cada vez que cambia o al desmontar (Escape,
  // click afuera, la X, además del "Volver" explícito) — así nunca se filtra memoria sin
  // depender de acordarse de revocar a mano en cada lugar que sale de la vista "recortar".
  useEffect(() => {
    if (!imagenParaRecortar) return undefined;
    return () => URL.revokeObjectURL(imagenParaRecortar);
  }, [imagenParaRecortar]);

  function limpiarImagenParaRecortar() {
    setImagenParaRecortar(null);
    setErrorModeracion(null);
    setFotoSinCaraConfirmar(null);
  }

  function irAMenu() {
    limpiarImagenParaRecortar();
    setVista('menu');
    dispatch(limpiarErrorPerfil());
  }

  function abrirRecorte(archivoOImagenSinRecortar) {
    setImagenParaRecortar(URL.createObjectURL(archivoOImagenSinRecortar));
    setErrorModeracion(null);
    setFotoSinCaraConfirmar(null);
    setVista('recortar');
  }

  function handleElegirArchivo(e) {
    const archivo = e.target.files?.[0];
    e.target.value = ''; // permite re-elegir el mismo archivo dos veces seguidas
    if (!archivo) return;
    if (!TIPOS_ACEPTADOS.includes(archivo.type)) {
      toast('La imagen debe ser JPG o PNG', { tipo: 'error' });
      return;
    }
    if (archivo.size > TAMANO_MAXIMO_BYTES) {
      toast('La imagen no puede superar los 8MB', { tipo: 'error' });
      return;
    }
    abrirRecorte(archivo);
  }

  async function subirArchivoRecortado(archivo) {
    return dispatch(subirFotoPropia(archivo)).unwrap()
      .then(() => {
        toast('Foto actualizada');
        limpiarImagenParaRecortar();
        setVista('menu');
      });
  }

  async function handleConfirmarRecorte(areaRecortePixels) {
    setProcesandoRecorte(true);
    setErrorModeracion(null);
    try {
      const imagen = await cargarImagen(imagenParaRecortar);
      const archivo = await recortarYComprimir(imagen, areaRecortePixels);
      const { caraDetectada, inapropiada } = await moderarFoto(archivo);
      if (inapropiada) {
        setErrorModeracion('Esta imagen no se puede usar como foto de perfil. Elegí otra.');
        return;
      }
      if (caraDetectada === false) {
        setFotoSinCaraConfirmar(archivo);
        return;
      }
      await subirArchivoRecortado(archivo);
    } catch {
      toast('No pudimos procesar la foto', { tipo: 'error' });
    } finally {
      setProcesandoRecorte(false);
    }
  }

  function handleSubirIgual() {
    if (!fotoSinCaraConfirmar) return;
    subirArchivoRecortado(fotoSinCaraConfirmar).catch(() => {});
  }

  function handleElegirAvatar(key) {
    dispatch(elegirAvatarPropio(key)).unwrap()
      .then(() => { toast('Avatar actualizado'); setVista('menu'); })
      .catch(() => {});
  }

  function handleQuitarFoto() {
    dispatch(quitarFotoPropia()).unwrap()
      .then(() => toast('Foto quitada'))
      .catch(() => {});
  }

  return (
    <Modal onCerrar={onCerrar} labelledBy="selector-foto-titulo">
      {vista === 'menu' && (
        <>
          <h2 id="selector-foto-titulo" className="modal-title">Tu foto de perfil</h2>
          <div style={{ display: 'flex', justifyContent: 'center', margin: '4px 0 22px' }}>
            <UsuarioAvatar
              fotoUrl="/api/usuarios/me/foto"
              tieneFoto={!!fotoInfo?.tieneFoto}
              avatarGenerico={fotoInfo?.avatarGenerico}
              nombreCompleto={usuario?.nombreCompleto}
              size={96}
              version={fotoVersion}
            />
          </div>
          {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>{error}</div>}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            <button type="button" className="btn btn-outline" disabled={guardando} onClick={() => inputArchivoRef.current?.click()}>
              {guardando && <span className="spinner" aria-hidden="true" />}
              Subir desde la galería
            </button>
            <input
              ref={inputArchivoRef}
              type="file"
              accept="image/jpeg,image/png"
              onChange={handleElegirArchivo}
              style={{ display: 'none' }}
            />
            <button type="button" className="btn btn-outline" disabled={guardando} onClick={() => setVista('camara')}>
              Sacarme una foto ahora
            </button>
            <button type="button" className="btn btn-outline" disabled={guardando} onClick={() => setVista('avatares')}>
              Elegir un avatar
            </button>
            {(fotoInfo?.tieneFoto || fotoInfo?.avatarGenerico) && (
              <button type="button" className="btn btn-outline" disabled={guardando} onClick={handleQuitarFoto}>
                Quitar foto
              </button>
            )}
          </div>
          <div className="modal-actions" style={{ marginTop: 22 }}>
            <button type="button" className="btn btn-outline" onClick={onCerrar}>Cerrar</button>
          </div>
        </>
      )}

      {vista === 'camara' && (
        <VistaCamara
          onVolver={irAMenu}
          onCapturada={abrirRecorte}
          guardando={guardando}
          error={error}
        />
      )}

      {vista === 'recortar' && (
        <>
          {!fotoSinCaraConfirmar && (
            <>
              <CropeadorFoto
                imagenUrl={imagenParaRecortar}
                onConfirmar={handleConfirmarRecorte}
                onCancelar={irAMenu}
                guardando={procesandoRecorte}
              />
              {errorModeracion && <div className="alert alert-error" role="alert" style={{ marginTop: 16 }}>{errorModeracion}</div>}
            </>
          )}
          {fotoSinCaraConfirmar && (
            <>
              <h2 id="selector-foto-titulo" className="modal-title">No detectamos tu cara con claridad</h2>
              <p style={{ color: 'var(--muted)', fontSize: 14.5 }}>Puede ser una foto de perfil sin cara a propósito, o quizás no salió bien encuadrada. ¿Querés usarla igual?</p>
              <div className="modal-actions" style={{ marginTop: 22 }}>
                <button type="button" className="btn btn-outline" disabled={guardando} onClick={() => setFotoSinCaraConfirmar(null)}>Volver a recortar</button>
                <button type="button" className="btn btn-primary" disabled={guardando} onClick={handleSubirIgual}>
                  {guardando && <span className="spinner" aria-hidden="true" />}
                  Subir igual
                </button>
              </div>
            </>
          )}
        </>
      )}

      {vista === 'avatares' && (
        <>
          <h2 id="selector-foto-titulo" className="modal-title">Elegí un avatar</h2>
          {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>{error}</div>}
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 22 }}>
            {AVATARES_GENERICOS.map((a) => (
              <button
                key={a.key}
                type="button"
                disabled={guardando}
                onClick={() => handleElegirAvatar(a.key)}
                aria-label={`Elegir este avatar`}
                style={{
                  width: '100%', aspectRatio: '1', borderRadius: '50%', border: fotoInfo?.avatarGenerico === a.key ? '3px solid var(--teal)' : '1px solid var(--line)',
                  background: colorDeAvatar(a.key), display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer',
                }}
              >
                <AvatarGenericoIcono style={{ width: '60%', height: '60%', color: '#fff' }} />
              </button>
            ))}
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-outline" onClick={irAMenu}>Volver</button>
          </div>
        </>
      )}
    </Modal>
  );
}

function VistaCamara({ onVolver, onCapturada, guardando, error }) {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const [errorCamara, setErrorCamara] = useState(null);
  const [capturaUrl, setCapturaUrl] = useState(null);

  useEffect(() => {
    // getUserMedia necesita un contexto seguro (https, o localhost en desarrollo) — si el
    // navegador ni siquiera expone la API, mejor avisar de una que dejar el botón "Capturar"
    // colgado sin explicación.
    if (!navigator.mediaDevices?.getUserMedia) {
      setErrorCamara('Este navegador no permite acceder a la cámara. Probá subir la foto desde la galería.');
      return undefined;
    }
    let cancelado = false;
    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user' }, audio: false })
      .then((stream) => {
        if (cancelado) { stream.getTracks().forEach((t) => t.stop()); return; }
        streamRef.current = stream;
        if (videoRef.current) videoRef.current.srcObject = stream;
      })
      .catch(() => setErrorCamara('No pudimos acceder a la cámara. Revisá los permisos del navegador, o subí la foto desde la galería.'));
    return () => {
      cancelado = true;
      streamRef.current?.getTracks().forEach((t) => t.stop());
    };
  }, []);

  function capturar() {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    setCapturaUrl(canvas.toDataURL('image/jpeg', 0.92));
  }

  function reintentar() {
    setCapturaUrl(null);
  }

  function confirmar() {
    canvasRef.current.toBlob((blob) => {
      if (!blob) return;
      onCapturada(new File([blob], 'foto-perfil.jpg', { type: 'image/jpeg' }));
    }, 'image/jpeg', 0.92);
  }

  return (
    <>
      <h2 id="selector-foto-titulo" className="modal-title">Sacate una foto</h2>
      {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>{error}</div>}
      {errorCamara && <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>{errorCamara}</div>}
      <div style={{ borderRadius: 'var(--radius-m)', overflow: 'hidden', background: 'var(--bg-soft)', aspectRatio: '4 / 3', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {capturaUrl ? (
          <img src={capturaUrl} alt="Vista previa de la foto capturada" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <video ref={videoRef} autoPlay playsInline muted style={{ width: '100%', height: '100%', objectFit: 'cover', transform: 'scaleX(-1)' }} />
        )}
      </div>
      <canvas ref={canvasRef} style={{ display: 'none' }} />
      <div className="modal-actions" style={{ marginTop: 22 }}>
        <button type="button" className="btn btn-outline" onClick={onVolver}>Volver</button>
        {capturaUrl ? (
          <>
            <button type="button" className="btn btn-outline" disabled={guardando} onClick={reintentar}>Volver a intentar</button>
            <button type="button" className="btn btn-primary" disabled={guardando} onClick={confirmar}>
              {guardando && <span className="spinner" aria-hidden="true" />}
              Usar esta foto
            </button>
          </>
        ) : (
          <button type="button" className="btn btn-primary" disabled={!!errorCamara} onClick={capturar}>Capturar</button>
        )}
      </div>
    </>
  );
}
