import { useEffect, useState } from 'react';
import axiosClient from '../api/axiosClient';
import { colorDeAvatar } from '../constants/avataresGenericos';
import AvatarGenericoIcono from './AvatarGenericoIcono';

// Caché de fotos compartida por todos los avatares de la pantalla. Antes cada avatar pedía su
// propia copia: con la foto propia visible en el header, en "Mi cuenta" y en el selector, se
// bajaba la misma imagen tres veces. Ahora el primer pedido se reutiliza (incluso mientras está
// en vuelo). La clave incluye el token de la sesión: "/api/usuarios/me/foto" es la foto de quien
// esté logueado, así que si cambia la cuenta no se puede reusar la imagen de la anterior.
const fotosEnCache = new Map(); // clave sin versión -> { version, promesa de object URL }

function cargarFoto(fotoUrl, version) {
  let sesion = '';
  try { sesion = localStorage.getItem('fkt_token') || ''; } catch { /* sin storage: no pasa nada */ }
  const clave = `${sesion}|${fotoUrl}`;
  const previa = fotosEnCache.get(clave);
  if (previa && previa.version === version) return previa.promesa;
  // Foto reemplazada (cambió la versión): se libera la URL anterior para no acumular memoria.
  previa?.promesa.then((url) => URL.revokeObjectURL(url)).catch(() => {});
  const promesa = axiosClient.get(fotoUrl, { responseType: 'blob' }).then((res) => URL.createObjectURL(res.data));
  const entrada = { version, promesa };
  fotosEnCache.set(clave, entrada);
  // Si falla, se saca de la caché para que el próximo avatar lo vuelva a intentar.
  promesa.catch(() => { if (fotosEnCache.get(clave) === entrada) fotosEnCache.delete(clave); });
  return promesa;
}

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
  // Qué foto corresponde mostrar ahora; el resultado guardado solo vale si es de esta misma
  // clave (si cambió la foto o la versión, se ignora el viejo sin tener que resetear estado).
  const clave = tieneFoto && fotoUrl ? `${fotoUrl}|${version ?? ''}` : null;
  const [resultado, setResultado] = useState({ clave: null, url: null, error: false });

  useEffect(() => {
    if (!clave) return undefined;
    let cancelado = false;
    cargarFoto(fotoUrl, version)
      .then((url) => { if (!cancelado) setResultado({ clave, url, error: false }); })
      .catch(() => { if (!cancelado) setResultado({ clave, url: null, error: true }); });
    return () => { cancelado = true; };
  }, [clave, fotoUrl, version]);

  const vigente = resultado.clave === clave ? resultado : { url: null, error: false };
  const blobUrl = vigente.url;
  const errorCarga = vigente.error;

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
