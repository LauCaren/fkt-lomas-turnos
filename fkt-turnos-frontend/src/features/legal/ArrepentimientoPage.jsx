import { useState } from 'react';
import { Link } from 'react-router-dom';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';
import Resultado from '../../components/ui/Resultado';
import { SITE_URL } from '../../config';

// "Botón de arrepentimiento" dentro de la app (el sitio tiene su propia versión en
// arrepentimiento.html, contra el mismo endpoint). Ley 24.240 art. 34 y Res. SCI 424/2020:
// tiene que funcionar SIN iniciar sesión — por eso es una ruta pública y no pide cuenta, aunque
// la persona esté logueada. El código del trámite se muestra apenas se manda (y llega por mail).

const VACIO = { nombreCompleto: '', dni: '', email: '', telefono: '', detalleOperacion: '', sitioWeb: '' };

export default function ArrepentimientoPage() {
  const [form, setForm] = useState(VACIO);
  const [enviando, setEnviando] = useState(false);
  const [error, setError] = useState(null);
  const [codigo, setCodigo] = useState(null);

  function cambiar(e) {
    setForm({ ...form, [e.target.name]: e.target.value });
  }

  async function enviar(e) {
    e.preventDefault();
    setEnviando(true);
    setError(null);
    try {
      const { data } = await axiosClient.post('/api/arrepentimiento', form);
      setCodigo(data.codigo);
    } catch (err) {
      setError(mensajeDeError(err, 'No pudimos registrar tu pedido. Probá de nuevo o escribinos por WhatsApp.'));
    } finally {
      setEnviando(false);
    }
  }

  if (codigo) {
    return (
      <Resultado estado="ok" titulo="Recibimos tu pedido">
        <p role="status">Este es tu código de trámite. También te lo mandamos por email: guardalo, es tu constancia.</p>
        <p className="codigo-tramite">{codigo}</p>
        <p>Te vamos a contactar para confirmarte la cancelación y, si pagaste online, la devolución por el mismo medio de pago.</p>
      </Resultado>
    );
  }

  return (
    <div className="acceso acceso-ancho">
      <div className="acceso-cabecera">
        <h1>Botón de arrepentimiento</h1>
        <p>
          Si sacaste un turno o pagaste una consulta por internet, tenés 10 días corridos para cancelarlo,
          sin costo y sin dar explicaciones (Ley 24.240, art. 34). No hace falta iniciar sesión.
        </p>
      </div>

      <form onSubmit={enviar} className="card acceso-hoja">
        {error && <div className="alert alert-error" role="alert">{error}</div>}
        <div className="field">
          <label htmlFor="nombreCompleto">Nombre y apellido</label>
          <input id="nombreCompleto" name="nombreCompleto" autoComplete="name" required maxLength={150} value={form.nombreCompleto} onChange={cambiar} />
        </div>
        <div className="field">
          <label htmlFor="dni">DNI <span className="campo-opcional">(opcional, nos ayuda a encontrar tu turno)</span></label>
          <input id="dni" name="dni" inputMode="numeric" pattern="\d{7,8}" maxLength={8} title="7 u 8 números, sin puntos" value={form.dni} onChange={cambiar} />
        </div>
        <div className="field">
          <label htmlFor="email">Email</label>
          <input id="email" name="email" type="email" autoComplete="email" required maxLength={180} value={form.email} onChange={cambiar} />
        </div>
        <div className="field">
          <label htmlFor="telefono">Teléfono <span className="campo-opcional">(opcional)</span></label>
          <input id="telefono" name="telefono" type="tel" autoComplete="tel" maxLength={30} value={form.telefono} onChange={cambiar} />
        </div>
        <div className="field">
          <label htmlFor="detalleOperacion">¿Qué querés cancelar?</label>
          <textarea id="detalleOperacion" name="detalleOperacion" rows={4} required maxLength={1000}
            placeholder="Por ejemplo: turno de traumatología del 20/10 a las 10 hs, pagado con Mercado Pago."
            value={form.detalleOperacion} onChange={cambiar} />
        </div>
        {/* Honeypot: invisible para personas (mismo criterio que el registro). */}
        <div className="honeypot" aria-hidden="true">
          <label htmlFor="sitioWeb">No completar este campo</label>
          <input id="sitioWeb" name="sitioWeb" type="text" tabIndex={-1} autoComplete="off" value={form.sitioWeb} onChange={cambiar} />
        </div>
        <button type="submit" className="btn btn-primary btn-block btn-lg" disabled={enviando}>
          {enviando && <span className="spinner" aria-hidden="true" />}
          {enviando ? 'Enviando…' : 'Enviar pedido de arrepentimiento'}
        </button>
        <p className="campo-ayuda">
          Usamos estos datos solo para gestionar tu pedido (ver la{' '}
          <a href={`${SITE_URL}/politica-de-privacidad.html`} target="_blank" rel="noopener noreferrer" className="link-fuerte">política de privacidad</a>).
        </p>
      </form>

      <div className="acceso-pie">
        <p>¿Preferís hablar con alguien? Escribinos por <a href="https://wa.me/5491123515402" target="_blank" rel="noopener noreferrer" className="link-fuerte">WhatsApp</a> o llamá al <a href="tel:+541159505888" className="link-fuerte">11 5950-5888</a>.</p>
        <p><Link to="/" className="link-fuerte">Volver al inicio</Link></p>
      </div>
    </div>
  );
}
