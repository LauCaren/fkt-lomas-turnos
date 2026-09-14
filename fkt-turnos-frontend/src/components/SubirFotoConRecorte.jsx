import { useEffect, useState } from 'react';
import Modal from './ui/Modal';
import CropeadorFoto from './CropeadorFoto';
import { cargarImagen, recortarYComprimir } from '../utils/imagenPerfil';
import { moderarFoto } from '../utils/moderacionFoto';

const TIPOS_ACEPTADOS = ['image/jpeg', 'image/png'];
const TAMANO_MAXIMO_BYTES = 8 * 1024 * 1024;

/**
 * Modal reusable de "elegí una imagen → recortala → se modera → se sube", para fotos públicas
 * que no son la de perfil propia (esa tiene su propio flujo más largo en SelectorFotoPerfil.jsx,
 * con cámara y avatares genéricos — acá alcanza con archivo + recorte). Primer uso: la foto
 * "tipo carnet" del médico en AdminMedicosTab.jsx, con aspect/cropShape de retrato en vez del
 * cuadrado circular de siempre.
 */
export default function SubirFotoConRecorte({ titulo, aspect = 1, cropShape = 'round', ancho, alto, onSubir, onCerrar }) {
  const [archivoOriginal, setArchivoOriginal] = useState(null);
  const [imagenUrl, setImagenUrl] = useState(null);
  const [procesando, setProcesando] = useState(false);
  const [error, setError] = useState(null);
  const [fotoSinCaraConfirmar, setFotoSinCaraConfirmar] = useState(null);

  useEffect(() => {
    if (!archivoOriginal) return undefined;
    const url = URL.createObjectURL(archivoOriginal);
    setImagenUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [archivoOriginal]);

  function handleElegirArchivo(e) {
    const archivo = e.target.files?.[0];
    e.target.value = '';
    if (!archivo) return;
    if (!TIPOS_ACEPTADOS.includes(archivo.type)) {
      setError('La imagen debe ser JPG o PNG');
      return;
    }
    if (archivo.size > TAMANO_MAXIMO_BYTES) {
      setError('La imagen no puede superar los 8MB');
      return;
    }
    setError(null);
    setArchivoOriginal(archivo);
  }

  async function subir(archivo) {
    setProcesando(true);
    try {
      await onSubir(archivo);
      onCerrar();
    } catch {
      setError('No se pudo subir la imagen');
    } finally {
      setProcesando(false);
    }
  }

  async function handleConfirmarRecorte(areaRecortePixels) {
    setProcesando(true);
    setError(null);
    try {
      const imagen = await cargarImagen(imagenUrl);
      const archivo = await recortarYComprimir(imagen, areaRecortePixels, ancho, alto);
      const { caraDetectada, inapropiada } = await moderarFoto(archivo);
      if (inapropiada) {
        setError('Esta imagen no se puede usar. Elegí otra.');
        return;
      }
      if (caraDetectada === false) {
        setFotoSinCaraConfirmar(archivo);
        return;
      }
      await subir(archivo);
    } catch {
      setError('No pudimos procesar la imagen');
    } finally {
      setProcesando(false);
    }
  }

  return (
    <Modal onCerrar={onCerrar} labelledBy="subir-foto-recorte-titulo">
      {!archivoOriginal && (
        <>
          <h2 id="subir-foto-recorte-titulo" className="modal-title">{titulo}</h2>
          {error && <div className="alert alert-error" role="alert" style={{ marginBottom: 16 }}>{error}</div>}
          <input type="file" accept="image/jpeg,image/png" onChange={handleElegirArchivo} style={{ marginBottom: 8 }} />
          <div className="modal-actions" style={{ marginTop: 22 }}>
            <button type="button" className="btn btn-outline" onClick={onCerrar}>Cerrar</button>
          </div>
        </>
      )}

      {archivoOriginal && !fotoSinCaraConfirmar && (
        <>
          <CropeadorFoto
            imagenUrl={imagenUrl}
            aspect={aspect}
            cropShape={cropShape}
            titulo={titulo}
            onConfirmar={handleConfirmarRecorte}
            onCancelar={() => { setArchivoOriginal(null); setError(null); }}
            guardando={procesando}
          />
          {error && <div className="alert alert-error" role="alert" style={{ marginTop: 16 }}>{error}</div>}
        </>
      )}

      {fotoSinCaraConfirmar && (
        <>
          <h2 id="subir-foto-recorte-titulo" className="modal-title">No detectamos una cara con claridad</h2>
          <p style={{ color: 'var(--muted)', fontSize: 14.5 }}>Puede que la imagen no esté bien encuadrada. ¿Querés usarla igual?</p>
          <div className="modal-actions" style={{ marginTop: 22 }}>
            <button type="button" className="btn btn-outline" disabled={procesando} onClick={() => setFotoSinCaraConfirmar(null)}>Volver a recortar</button>
            <button type="button" className="btn btn-primary" disabled={procesando} onClick={() => subir(fotoSinCaraConfirmar)}>
              {procesando && <span className="spinner" aria-hidden="true" />}
              Subir igual
            </button>
          </div>
        </>
      )}
    </Modal>
  );
}
