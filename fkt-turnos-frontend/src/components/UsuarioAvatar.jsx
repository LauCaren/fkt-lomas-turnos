import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';
import { colorDeAvatar } from '../constants/avataresGenericos';
import AvatarGenericoIcono from './AvatarGenericoIcono';

/**
 * Dibuja el avatar de un usuario cualquiera (uno mismo o alguien que se está mirando, p. ej.
 * un paciente desde la ficha de secretaría): foto real si tiene, si no el avatar genérico
 * elegido, si no las iniciales del nombre. `fotoUrl`/`tieneFoto`/`avatarGenerico` vienen del
 * llamador — de UsuarioFotoInfoDto para "mi" foto, o embebidos en PacienteBusquedaDto para la
 * foto de otra persona (ver AdminBusquedaPacienteTab / la ficha de paciente).
 *
 * El <img> no puede pedir la foto directo (el navegador no manda el header Authorization en
 * un src de imagen, y esa ruta no es pública como la de marketing de los médicos) — se pide
 * con axios, que sí lo manda, y se arma un object URL con el blob que vuelve.
 *
 * `version` es opcional — lo pasan Layout/SelectorFotoPerfil con perfilSlice.fotoVersion para
 * la foto propia. Sin esto, reemplazar una foto por otra no dispara un refetch: `fotoUrl` y
 * `tieneFoto` quedan idénticos antes y después (true → true), así que el efecto de abajo no
 * se re-ejecuta y el header sigue mostrando el blob viejo hasta recargar la página entera.
 */
export default function UsuarioAvatar({ fotoUrl, tieneFoto, avatarGenerico, nombreCompleto, size = 40, version }) {
  const [blobUrl, setBlobUrl] = useState(null);
  const [errorCarga, setErrorCarga] = useState(false);

  useEffect(() => {
    setBlobUrl(null);
    setErrorCarga(false);
    if (!tieneFoto || !fotoUrl) return undefined;
    let cancelado = false;
    let url = null;
    axiosClient.get(fotoUrl, { responseType: 'blob' })
      .then((res) => {
        if (cancelado) return;
        url = URL.createObjectURL(res.data);
        setBlobUrl(url);
      })
      .catch(() => { if (!cancelado) setErrorCarga(true); });
    return () => {
      cancelado = true;
      if (url) URL.revokeObjectURL(url);
    };
  }, [fotoUrl, tieneFoto, version]);

  const base = {
    width: size, height: size, borderRadius: '50%', flexShrink: 0,
    display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'hidden',
    border: '1px solid var(--line)',
  };

  if (tieneFoto && !errorCarga) {
    if (blobUrl) {
      return <img src={blobUrl} alt="" style={{ ...base, objectFit: 'cover' }} />;
    }
    return <div style={{ ...base, background: 'var(--bg-soft)' }} aria-hidden="true" />;
  }

  if (avatarGenerico) {
    return (
      <div style={{ ...base, background: colorDeAvatar(avatarGenerico) }}>
        <AvatarGenericoIcono style={{ width: '62%', height: '62%', color: '#fff' }} />
      </div>
    );
  }

  const iniciales = (nombreCompleto || '?').trim().split(/\s+/).filter(Boolean).slice(0, 2).map((p) => p[0]).join('').toUpperCase();
  return (
    <div style={{ ...base, background: 'var(--ink-soft)', color: '#fff', fontSize: Math.round(size * 0.4), fontWeight: 700 }}>
      {iniciales || '?'}
    </div>
  );
}
