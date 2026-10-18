// Script propio de trabaja-con-nosotros.html (antes estaba embebido en el HTML; se movió a un archivo
// aparte para que la Content-Security-Policy del sitio pueda prohibir scripts inline —
// ver _headers). Se carga después de config.js y site.js.

// URL del backend: se configura una sola vez para todo el sitio en assets/js/config.js.
const API_URL = window.FKT_CONFIG.apiUrl;
const TAMANO_MAXIMO = 5 * 1024 * 1024;

const form = document.getElementById('formPostulacion');
const mensajeBox = document.getElementById('postulacionMensaje');
const btn = document.getElementById('btnEnviarPostulacion');
const cvInput = document.getElementById('cv');

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  mensajeBox.innerHTML = '';

  const archivo = cvInput.files[0];
  if (archivo && archivo.size > TAMANO_MAXIMO) {
    mensajeBox.innerHTML = '<div class="alert alert-error">El CV no puede superar los 5MB.</div>';
    return;
  }

  btn.disabled = true;
  btn.textContent = 'Enviando…';

  const formData = new FormData();
  formData.append('nombreCompleto', document.getElementById('nombreCompleto').value.trim());
  formData.append('email', document.getElementById('email').value.trim());
  formData.append('telefono', document.getElementById('telefono').value.trim());
  formData.append('mensaje', document.getElementById('mensaje').value.trim());
  formData.append('sitioWeb', document.getElementById('sitioWeb').value);
  formData.append('cv', archivo);

  try {
    const res = await fetch(`${API_URL}/api/postulaciones`, { method: 'POST', body: formData });
    if (!res.ok) throw new Error();
    mensajeBox.innerHTML = '<div class="alert alert-success">¡Gracias por postularte! Recibimos tu CV y lo vamos a revisar.</div>';
    form.reset();
  } catch {
    mensajeBox.innerHTML = '<div class="alert alert-error">No pudimos enviar tu postulación. Probá de nuevo en unos minutos.</div>';
  } finally {
    btn.disabled = false;
    btn.textContent = 'Enviar postulación';
  }
});
