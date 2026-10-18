// Script propio de alquiler-consultorios.html (antes estaba embebido en el HTML; se movió a un archivo
// aparte para que la Content-Security-Policy del sitio pueda prohibir scripts inline —
// ver _headers). Se carga después de config.js y site.js.

// URL del backend: se configura una sola vez para todo el sitio en assets/js/config.js.
const API_URL = window.FKT_CONFIG.apiUrl;

const form = document.getElementById('formAlquiler');
const mensajeBox = document.getElementById('alquilerMensaje');
const btn = document.getElementById('btnEnviarAlquiler');

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  mensajeBox.innerHTML = '';
  btn.disabled = true;
  btn.textContent = 'Enviando…';

  const payload = {
    nombre: document.getElementById('nombre').value.trim(),
    email: document.getElementById('email').value.trim(),
    telefono: document.getElementById('telefono').value.trim(),
    mensaje: document.getElementById('mensaje').value.trim(),
    sitioWeb: document.getElementById('sitioWeb').value,
  };

  try {
    const res = await fetch(`${API_URL}/api/contacto`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) throw new Error();
    mensajeBox.innerHTML = '<div class="alert alert-success">¡Gracias! Recibimos tu consulta y te vamos a contactar a la brevedad.</div>';
    form.reset();
  } catch {
    mensajeBox.innerHTML = '<div class="alert alert-error">No pudimos enviar tu consulta. Probá de nuevo o escribinos por WhatsApp.</div>';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Enviar consulta';
  }
});
