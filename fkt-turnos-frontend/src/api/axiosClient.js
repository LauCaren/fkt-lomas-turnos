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
    }
    return Promise.reject(error);
  }
);

export function mensajeDeError(error, fallback = 'Ocurrió un error inesperado') {
  return error?.response?.data?.mensaje || fallback;
}

export default axiosClient;
