import { useState } from 'react';
import Cropper from 'react-easy-crop';

/**
 * Recorte cuadrado con zoom/arrastre antes de guardar la foto de perfil — se usa tanto para una
 * imagen elegida desde la galería como para una recién sacada con la cámara (ver
 * SelectorFotoPerfil.jsx), para que en los dos casos el usuario pueda centrar bien la cara antes
 * de que se comprima y se mande a moderar.
 */
export default function CropeadorFoto({ imagenUrl, onConfirmar, onCancelar, guardando }) {
  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [areaRecortePixels, setAreaRecortePixels] = useState(null);

  return (
    <>
      <h2 id="selector-foto-titulo" className="modal-title">Ajustá tu foto</h2>
      <div style={{ position: 'relative', width: '100%', aspectRatio: '1', borderRadius: 'var(--radius-m)', overflow: 'hidden', background: 'var(--bg-soft)' }}>
        <Cropper
          image={imagenUrl}
          crop={crop}
          zoom={zoom}
          aspect={1}
          cropShape="round"
          showGrid={false}
          onCropChange={setCrop}
          onZoomChange={setZoom}
          onCropComplete={(_, pixels) => setAreaRecortePixels(pixels)}
        />
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 16 }}>
        <span style={{ fontSize: 13, color: 'var(--muted)' }}>Zoom</span>
        <input
          type="range" min={1} max={3} step={0.01} value={zoom}
          onChange={(e) => setZoom(Number(e.target.value))}
          style={{ flex: 1 }}
          aria-label="Zoom de la foto"
        />
      </div>
      <div className="modal-actions" style={{ marginTop: 22 }}>
        <button type="button" className="btn btn-outline" disabled={guardando} onClick={onCancelar}>Volver</button>
        <button
          type="button"
          className="btn btn-primary"
          disabled={guardando || !areaRecortePixels}
          onClick={() => onConfirmar(areaRecortePixels)}
        >
          {guardando && <span className="spinner" aria-hidden="true" />}
          {guardando ? 'Revisando…' : 'Usar esta foto'}
        </button>
      </div>
    </>
  );
}
