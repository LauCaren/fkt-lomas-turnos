import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

// Foto de perfil propia — de cualquier rol (paciente, admin, secretaria, médico o contador),
// por eso vive en su propio slice en vez de colgar de adminSlice/cuentaSlice, que son
// específicos de un panel/rol. Layout.jsx la carga una vez por sesión, apenas hay usuario logueado.
const initialState = {
  fotoInfo: null, // { tieneFoto, avatarGenerico } — null hasta el primer fetch
  fotoVersion: 0, // se incrementa en cada cambio para forzar el refetch del blob en UsuarioAvatar
  status: 'idle',
  error: null,
};

export const cargarFotoInfo = createAsyncThunk('perfil/cargarFotoInfo', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/usuarios/me/foto-info');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No pudimos cargar tu foto de perfil'));
  }
});

export const subirFotoPropia = createAsyncThunk('perfil/subirFotoPropia', async (archivo, { rejectWithValue }) => {
  try {
    const formData = new FormData();
    formData.append('foto', archivo);
    const { data } = await axiosClient.post('/api/usuarios/me/foto', formData);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No pudimos guardar la foto'));
  }
});

export const elegirAvatarPropio = createAsyncThunk('perfil/elegirAvatarPropio', async (avatarKey, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/usuarios/me/avatar', { avatarKey });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No pudimos guardar el avatar'));
  }
});

export const quitarFotoPropia = createAsyncThunk('perfil/quitarFotoPropia', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.delete('/api/usuarios/me/foto');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No pudimos quitar la foto'));
  }
});

const perfilSlice = createSlice({
  name: 'perfil',
  initialState,
  reducers: {
    limpiarErrorPerfil(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(cargarFotoInfo.fulfilled, (state, action) => { state.fotoInfo = action.payload; state.fotoVersion += 1; })
      .addCase(subirFotoPropia.pending, (state) => { state.status = 'loading'; state.error = null; })
      .addCase(subirFotoPropia.fulfilled, (state, action) => { state.status = 'idle'; state.fotoInfo = action.payload; state.fotoVersion += 1; })
      .addCase(subirFotoPropia.rejected, (state, action) => { state.status = 'idle'; state.error = action.payload; })
      .addCase(elegirAvatarPropio.pending, (state) => { state.status = 'loading'; state.error = null; })
      .addCase(elegirAvatarPropio.fulfilled, (state, action) => { state.status = 'idle'; state.fotoInfo = action.payload; state.fotoVersion += 1; })
      .addCase(elegirAvatarPropio.rejected, (state, action) => { state.status = 'idle'; state.error = action.payload; })
      .addCase(quitarFotoPropia.pending, (state) => { state.status = 'loading'; state.error = null; })
      .addCase(quitarFotoPropia.fulfilled, (state, action) => { state.status = 'idle'; state.fotoInfo = action.payload; state.fotoVersion += 1; })
      .addCase(quitarFotoPropia.rejected, (state, action) => { state.status = 'idle'; state.error = action.payload; });
  },
});

export const { limpiarErrorPerfil } = perfilSlice.actions;
export default perfilSlice.reducer;
