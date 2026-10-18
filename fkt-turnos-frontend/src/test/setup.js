import '@testing-library/jest-dom/vitest';

// jsdom no implementa window.scrollTo (lo usa el wizard de reserva al cambiar de paso): sin
// esto cada test imprime "Not implemented" y ensucia la salida.
window.scrollTo = () => {};
