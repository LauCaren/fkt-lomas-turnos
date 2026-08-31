import { useEffect, useState } from 'react';

const CLAVE_VISTO = 'fkt_instalar_app_visto';

function detectarPlataforma() {
  const ua = navigator.userAgent;
  const esIOS = /iPad|iPhone|iPod/.test(ua) && !window.MSStream;
  const esAndroid = /Android/.test(ua);
  const esMobile = esIOS || esAndroid || /Mobi/.test(ua);
  const yaInstalada = window.matchMedia('(display-mode: standalone)').matches || window.navigator.standalone === true;
  return { esIOS, esAndroid, esMobile, yaInstalada };
}

function estadoInicial() {
  const yaVisto = localStorage.getItem(CLAVE_VISTO) === 'true';
  const info = detectarPlataforma();
  const mostrar = !yaVisto && info.esMobile && !info.yaInstalada;
  if (mostrar) localStorage.setItem(CLAVE_VISTO, 'true');
  return { plataforma: info, visible: mostrar };
}

export default function InstalarAppBanner() {
  const [{ plataforma, visible: visibleInicial }] = useState(estadoInicial);
  const [visible, setVisible] = useState(visibleInicial);
  const [eventoInstalacion, setEventoInstalacion] = useState(null);

  useEffect(() => {
    function handleBeforeInstallPrompt(e) {
      e.preventDefault();
      setEventoInstalacion(e);
    }
    window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
    return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
  }, []);

  function handleCerrar() {
    setVisible(false);
  }

  async function handleInstalar() {
    if (!eventoInstalacion) return;
    eventoInstalacion.prompt();
    await eventoInstalacion.userChoice;
    setEventoInstalacion(null);
    setVisible(false);
  }

  if (!visible) return null;

  return (
    <div className="card" style={{ marginBottom: 24, background: 'var(--teal-tint)', border: '1px solid var(--teal)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div>
          <div style={{ fontWeight: 700, fontSize: 15.5, marginBottom: 6 }}>📲 Agregá FKT Turnos a tu pantalla de inicio</div>
          {plataforma.esIOS ? (
            <p style={{ fontSize: 14, color: 'var(--ink-soft)', margin: 0 }}>
              Así la tenés a mano como una app: tocá el botón <strong>Compartir</strong> (el cuadrado con la flecha hacia arriba, abajo en Safari) y elegí <strong>"Agregar a pantalla de inicio"</strong>.
            </p>
          ) : (
            <p style={{ fontSize: 14, color: 'var(--ink-soft)', margin: 0 }}>
              Así la tenés a mano como una app: tocá el menú <strong>⋮</strong> de Chrome y elegí <strong>"Agregar a pantalla de inicio"</strong> o <strong>"Instalar app"</strong>.
            </p>
          )}
        </div>
        <button
          type="button"
          onClick={handleCerrar}
          aria-label="Cerrar"
          style={{ flexShrink: 0, width: 28, height: 28, borderRadius: '50%', border: 'none', background: 'rgba(255,255,255,0.6)', fontSize: 16, lineHeight: 1, cursor: 'pointer', color: 'var(--ink-soft)' }}
        >
          &times;
        </button>
      </div>
      {plataforma.esAndroid && eventoInstalacion && (
        <button type="button" className="btn btn-primary" style={{ marginTop: 14, padding: '9px 18px', fontSize: 13.5 }} onClick={handleInstalar}>
          Instalar ahora
        </button>
      )}
    </div>
  );
}
