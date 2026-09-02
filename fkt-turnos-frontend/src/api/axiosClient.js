import axios from 'axios';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

axiosClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('fkt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

axiosClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('fkt_token');
      localStorage.removeItem('fkt_user');
      // No podemos dispatchear logout() del store directo acá (import circular:
      // authSlice ya importa este archivo para sus propios thunks) — un evento del
      // DOM lo desacopla. Sin esto, el store seguía con token/usuario en memoria
      // después de un 401, así que la pantalla protegida se quedaba renderizada
      // como si la sesión siguiera viva mientras cada acción fallaba en silencio.
      window.dispatchEvent(new Event('fkt:sesion-vencida'));
    }
    return Promise.reject(error);
  }
);

export function mensajeDeError(error, fallback = 'Ocurrió un error inesperado') {
  return error?.response?.data?.mensaje || fallback;
}

export default axiosClient;
