import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

// Estado de Redux de la caja del admin (ver AdminCajaTab): resumen, ingresos, retiros, gastos y
// categorías de gasto. El contador tiene su propio slice de solo lectura (contadorSlice).

export const fetchResumenCaja = createAsyncThunk('caja/fetchResumen', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/caja/resumen');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchIngresos = createAsyncThunk('caja/fetchIngresos', async ({ medicoId, desde, hasta } = {}, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/caja/ingresos', { params: { medicoId: medicoId || undefined, desde: desde || undefined, hasta: hasta || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const registrarIngreso = createAsyncThunk('caja/registrarIngreso', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/caja/ingresos', payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo registrar el ingreso'));
  }
});

export const anularIngreso = createAsyncThunk('caja/anularIngreso', async ({ id, motivo }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/caja/ingresos/${id}/anular`, { motivo });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo anular el ingreso'));
  }
});

export const fetchRetiros = createAsyncThunk('caja/fetchRetiros', async ({ medicoId } = {}, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/caja/retiros', { params: { medicoId: medicoId || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const registrarRetiro = createAsyncThunk('caja/registrarRetiro', async (payload, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/caja/retiros', payload);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo registrar el retiro'));
  }
});

export const anularRetiro = createAsyncThunk('caja/anularRetiro', async ({ id, motivo }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/caja/retiros/${id}/anular`, { motivo });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo anular el retiro'));
  }
});

export const fetchCategoriasGasto = createAsyncThunk('caja/fetchCategoriasGasto', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/caja/gastos/categorias');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const crearCategoriaGasto = createAsyncThunk('caja/crearCategoriaGasto', async (nombre, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/caja/gastos/categorias', { nombre });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo crear la categoría'));
  }
});

export const fetchGastos = createAsyncThunk('caja/fetchGastos', async ({ categoriaId, desde, hasta } = {}, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/admin/caja/gastos', { params: { categoriaId: categoriaId || undefined, desde: desde || undefined, hasta: hasta || undefined } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const registrarGasto = createAsyncThunk('caja/registrarGasto', async (formData, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.post('/api/admin/caja/gastos', formData);
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo registrar el gasto'));
  }
});

export const anularGasto = createAsyncThunk('caja/anularGasto', async ({ id, motivo }, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.put(`/api/admin/caja/gastos/${id}/anular`, { motivo });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error, 'No se pudo anular el gasto'));
  }
});

const cajaSlice = createSlice({
  name: 'caja',
  initialState: {
    resumen: null,
    ingresos: [],
    retiros: [],
    categorias: [],
    gastos: [],
    error: null,
    mensajeExito: null,
  },
  reducers: {
    limpiarMensajesCaja(state) {
      state.error = null;
      state.mensajeExito = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchResumenCaja.fulfilled, (state, action) => { state.resumen = action.payload; })
      .addCase(fetchResumenCaja.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchIngresos.fulfilled, (state, action) => { state.ingresos = action.payload; })
      .addCase(fetchIngresos.rejected, (state, action) => { state.error = action.payload; })
      .addCase(registrarIngreso.fulfilled, (state, action) => {
        state.ingresos = [action.payload, ...state.ingresos];
        state.mensajeExito = 'Ingreso registrado';
      })
      .addCase(registrarIngreso.rejected, (state, action) => { state.error = action.payload; })
      .addCase(anularIngreso.fulfilled, (state, action) => {
        state.ingresos = state.ingresos.map((i) => i.id === action.payload.id ? action.payload : i);
        state.mensajeExito = 'Ingreso anulado';
      })
      .addCase(anularIngreso.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchRetiros.fulfilled, (state, action) => { state.retiros = action.payload; })
      .addCase(fetchRetiros.rejected, (state, action) => { state.error = action.payload; })
      .addCase(registrarRetiro.fulfilled, (state, action) => {
        state.retiros = [action.payload, ...state.retiros];
        state.mensajeExito = 'Retiro registrado';
      })
      .addCase(registrarRetiro.rejected, (state, action) => { state.error = action.payload; })
      .addCase(anularRetiro.fulfilled, (state, action) => {
        state.retiros = state.retiros.map((r) => r.id === action.payload.id ? action.payload : r);
        state.mensajeExito = 'Retiro anulado';
      })
      .addCase(anularRetiro.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchCategoriasGasto.fulfilled, (state, action) => { state.categorias = action.payload; })
      .addCase(crearCategoriaGasto.fulfilled, (state, action) => { state.categorias = [...state.categorias, action.payload]; })
      .addCase(crearCategoriaGasto.rejected, (state, action) => { state.error = action.payload; })
      .addCase(fetchGastos.fulfilled, (state, action) => { state.gastos = action.payload; })
      .addCase(fetchGastos.rejected, (state, action) => { state.error = action.payload; })
      .addCase(registrarGasto.fulfilled, (state, action) => {
        state.gastos = [action.payload, ...state.gastos];
        state.mensajeExito = 'Gasto registrado';
      })
      .addCase(registrarGasto.rejected, (state, action) => { state.error = action.payload; })
      .addCase(anularGasto.fulfilled, (state, action) => {
        state.gastos = state.gastos.map((g) => g.id === action.payload.id ? action.payload : g);
        state.mensajeExito = 'Gasto anulado';
      })
      .addCase(anularGasto.rejected, (state, action) => { state.error = action.payload; });
  },
});

export const { limpiarMensajesCaja } = cajaSlice.actions;
export default cajaSlice.reducer;
