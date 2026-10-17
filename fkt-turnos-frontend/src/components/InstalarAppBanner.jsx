import { useEffect, useState } from 'react';
import { EllipsisVertical, Smartphone, X } from 'lucide-react';

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
    <aside className="instalar-app" aria-label="Instalar la app">
      <span className="instalar-app-icono" aria-hidden="true"><Smartphone size={22} /></span>
      <div className="instalar-app-texto">
        <p className="instalar-app-titulo">Agregá FKT Turnos a tu pantalla de inicio</p>
        {plataforma.esIOS ? (
          <p>
            Así la tenés a mano como una app: tocá <strong>Compartir</strong> (el cuadrado con la flecha hacia arriba, abajo en Safari) y elegí <strong>"Agregar a pantalla de inicio"</strong>.
          </p>
        ) : (
          <p>
            Así la tenés a mano como una app: tocá el menú <EllipsisVertical size={15} aria-label="de tres puntos" style={{ verticalAlign: -2 }} /> de Chrome y elegí <strong>"Agregar a pantalla de inicio"</strong> o <strong>"Instalar app"</strong>.
          </p>
        )}
        {plataforma.esAndroid && eventoInstalacion && (
          <button type="button" className="btn btn-primary" style={{ marginTop: 12 }} onClick={handleInstalar}>
            Instalar ahora
          </button>
        )}
      </div>
      <button type="button" onClick={handleCerrar} aria-label="Cerrar" className="instalar-app-cerrar">
        <X size={20} aria-hidden="true" />
      </button>
    </aside>
  );
}
