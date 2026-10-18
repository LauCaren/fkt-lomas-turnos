// Script propio de 404.html (antes estaba embebido en el HTML; se movió a un archivo
// aparte para que la Content-Security-Policy del sitio pueda prohibir scripts inline —
// ver _headers). Se carga después de config.js y site.js.

document.getElementById('error404SearchBtn').addEventListener('click', () => {
  if (window.fktOpenSearch) window.fktOpenSearch();
});
