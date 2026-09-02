import axiosClient from './axiosClient';

/**
 * Descarga un archivo de un endpoint admin-protegido (necesita el header Authorization que
 * pone axiosClient — un <a href> plano no lo manda) y dispara la descarga en el navegador.
 */
export async function descargarArchivo(url, params, nombreSugerido) {
  const response = await axiosClient.get(url, { params, responseType: 'blob' });
  const contentDisposition = response.headers['content-disposition'];
  const nombre = extraerNombreArchivo(contentDisposition) || nombreSugerido;

  const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: response.headers['content-type'] }));
  const link = document.createElement('a');
  link.href = blobUrl;
  link.setAttribute('download', nombre);
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(blobUrl);
}

function extraerNombreArchivo(contentDisposition) {
  if (!contentDisposition) return null;
  const match = contentDisposition.match(/filename="?([^"]+)"?/);
  return match ? match[1] : null;
}

/**
 * Abre un archivo de un endpoint admin-protegido en una pestaña nueva en vez de
 * descargarlo (para "ver" una orden médica o un comprobante sin guardarlo).
 */
export async function verArchivo(url) {
  const response = await axiosClient.get(url, { responseType: 'blob' });
  const blobUrl = window.URL.createObjectURL(new Blob([response.data], { type: response.headers['content-type'] }));
  window.open(blobUrl, '_blank', 'noopener');
  setTimeout(() => window.URL.revokeObjectURL(blobUrl), 60000);
}
