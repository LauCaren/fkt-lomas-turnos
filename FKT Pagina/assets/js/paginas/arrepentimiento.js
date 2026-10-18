// Formulario del "Botón de arrepentimiento" (arrepentimiento.html). Ley 24.240 art. 34 y
// Resolución SCI 424/2020: sin registro ni login, y con un código de trámite que se le muestra a
// la persona apenas lo manda (además le llega por email — ver ArrepentimientoService en el
// backend). Se carga después de config.js y site.js.

const API_URL = window.FKT_CONFIG.apiUrl;

const form = document.getElementById('formArrepentimiento');
const mensajeBox = document.getElementById('arrepentimientoMensaje');
const btn = document.getElementById('btnEnviarArrepentimiento');
const TEXTO_BOTON = btn.textContent;

function mostrarAlerta(tipo, ...nodos) {
  const alerta = document.createElement('div');
  alerta.className = `alert alert-${tipo}`;
  alerta.setAttribute('role', tipo === 'error' ? 'alert' : 'status');
  alerta.append(...nodos);
  mensajeBox.replaceChildren(alerta);
  alerta.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!form.reportValidity()) return;

  btn.disabled = true;
  btn.textContent = 'Enviando…';

  const valor = (id) => document.getElementById(id).value.trim();
  const payload = {
    nombreCompleto: valor('nombreCompleto'),
    dni: valor('dni'),
    email: valor('email'),
    telefono: valor('telefono'),
    detalleOperacion: valor('detalleOperacion'),
    sitioWeb: document.getElementById('sitioWeb').value,
  };

  try {
    const res = await fetch(`${API_URL}/api/arrepentimiento`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    const datos = await res.json().catch(() => ({}));
    if (!res.ok) {
      // Los errores de validación vienen campo por campo en "detalle"; si no, el mensaje general.
      const detalle = datos.detalle ? Object.values(datos.detalle).join(' ') : null;
      throw new Error(detalle || datos.mensaje || 'No pudimos registrar tu pedido.');
    }

    // textContent (no innerHTML): el código viene del servidor, pero no hace falta confiar en él.
    const codigo = document.createElement('strong');
    codigo.className = 'codigo-tramite';
    codigo.textContent = datos.codigo;
    mostrarAlerta('success',
      'Recibimos tu pedido. Este es tu código de trámite (también te lo mandamos por email):',
      codigo,
      'Te vamos a contactar para confirmarte la cancelación y, si pagaste online, la devolución.');
    form.reset();
  } catch (error) {
    mostrarAlerta('error', `${error.message} Probá de nuevo o pedilo por WhatsApp al 11 2351-5402.`);
  } finally {
    btn.disabled = false;
    btn.textContent = TEXTO_BOTON;
  }
});
