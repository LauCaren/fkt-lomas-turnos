import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import axiosClient, { mensajeDeError } from '../../api/axiosClient';

export const fetchResumenCajaContador = createAsyncThunk('contador/fetchResumen', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/contador/caja/resumen');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchIngresosContador = createAsyncThunk('contador/fetchIngresos', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/contador/caja/ingresos');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchRetirosContador = createAsyncThunk('contador/fetchRetiros', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/contador/caja/retiros');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchGastosContador = createAsyncThunk('contador/fetchGastos', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/contador/caja/gastos');
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

export const fetchTendenciaContador = createAsyncThunk('contador/fetchTendencia', async (dias, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get('/api/contador/caja/tendencia', { params: { dias } });
    return data;
  } catch (error) {
    return rejectWithValue(mensajeDeError(error));
  }
});

const contadorSlice = createSlice({
  name: 'contador',
  initialState: {
    resumen: null,
    resumenCargando: false,
    ingresos: [],
    ingresosCargando: false,
    retiros: [],
    retirosCargando: false,
    gastos: [],
    gastosCargando: false,
    tendencia: [],
    error: null,
  },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(fetchResumenCajaContador.pending, (state) => { state.resumenCargando = true; })
      .addCase(fetchResumenCajaContador.fulfilled, (state, action) => { state.resumen = action.payload; state.resumenCargando = false; })
      .addCase(fetchResumenCajaContador.rejected, (state, action) => { state.error = action.payload; state.resumenCargando = false; })
      .addCase(fetchIngresosContador.pending, (state) => { state.ingresosCargando = true; })
      .addCase(fetchIngresosContador.fulfilled, (state, action) => { state.ingresos = action.payload; state.ingresosCargando = false; })
      .addCase(fetchIngresosContador.rejected, (state, action) => { state.error = action.payload; state.ingresosCargando = false; })
      .addCase(fetchRetirosContador.pending, (state) => { state.retirosCargando = true; })
      .addCase(fetchRetirosContador.fulfilled, (state, action) => { state.retiros = action.payload; state.retirosCargando = false; })
      .addCase(fetchRetirosContador.rejected, (state, action) => { state.error = action.payload; state.retirosCargando = false; })
      .addCase(fetchGastosContador.pending, (state) => { state.gastosCargando = true; })
      .addCase(fetchGastosContador.fulfilled, (state, action) => { state.gastos = action.payload; state.gastosCargando = false; })
      .addCase(fetchGastosContador.rejected, (state, action) => { state.error = action.payload; state.gastosCargando = false; })
      .addCase(fetchTendenciaContador.fulfilled, (state, action) => { state.tendencia = action.payload; })
      .addCase(fetchTendenciaContador.rejected, (state, action) => { state.error = action.payload; })
      // Cada pedido nuevo limpia el error del anterior: sin esto, una falla momentánea dejaba el
      // cartel de error pegado en todas las pestañas aunque los datos ya hubieran cargado bien.
      .addMatcher((action) => action.type.startsWith('contador/') && action.type.endsWith('/pending'), (state) => { state.error = null; });
  },
});

export default contadorSlice.reducer;
