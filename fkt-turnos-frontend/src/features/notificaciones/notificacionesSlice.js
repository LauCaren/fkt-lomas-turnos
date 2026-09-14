import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

// Campanita del header (Layout.jsx) — cualquier rol logueado, ver NotificacionesBell.jsx.
const initialState = {
  items: [],
  cantidadNoLeidas: 0,
  status: 'idle',
  error: null,
};

export const fetchNotificaciones = createAsyncThunk('notificaciones/fetch', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/notificaciones', { params: { size: 20 } });
    return data.content;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No pudimos cargar tus notificaciones'));
  }
});

// Separado de fetchNotificaciones a propósito — la campanita pollea solo este número liviano
// cada tanto, sin traer la lista completa hasta que el usuario la abre (ver useEffect de polling
// en NotificacionesBell.jsx).
export const fetchCantidadNoLeidas = createAsyncThunk('notificaciones/fetchCantidad', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/notificaciones/no-leidas/cantidad');
    return data.cantidad;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo actualizar la cantidad'));
  }
});

export const marcarNotificacionLeida = createAsyncThunk('notificaciones/marcarLeida', async (id, { rejectWithValue }) => {
  try {
    await axiosClient.put(`/api/notificaciones/${id}/leida`);
    return id;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo marcar como leída'));
  }
});

export const marcarTodasLeidas = createAsyncThunk('notificaciones/marcarTodasLeidas', async (_, { rejectWithValue }) => {
  try {
    await axiosClient.put('/api/notificaciones/marcar-todas-leidas');
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo marcar todo como leído'));
  }
});

const notificacionesSlice = createSlice({
  name: 'notificaciones',
  initialState,
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchNotificaciones.pending, (state) => { state.status = 'loading'; })
      .addCase(fetchNotificaciones.fulfilled, (state, action) => { state.status = 'idle'; state.items = action.payload; })
      .addCase(fetchNotificaciones.rejected, (state, action) => { state.status = 'idle'; state.error = action.payload; })
      .addCase(fetchCantidadNoLeidas.fulfilled, (state, action) => { state.cantidadNoLeidas = action.payload; })
      .addCase(marcarNotificacionLeida.fulfilled, (state, action) => {
        const item = state.items.find((n) => n.id === action.payload);
        if (item && !item.leida) {
          item.leida = true;
          state.cantidadNoLeidas = Math.max(0, state.cantidadNoLeidas - 1);
        }
      })
      .addCase(marcarTodasLeidas.fulfilled, (state) => {
        state.items.forEach((n) => { n.leida = true; });
        state.cantidadNoLeidas = 0;
      });
  },
});

export default notificacionesSlice.reducer;
