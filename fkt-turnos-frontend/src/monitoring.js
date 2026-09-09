import * as Sentry from '@sentry/react';

// Sin VITE_SENTRY_DSN configurada (el default en desarrollo y hasta que se cree una
// cuenta real de Sentry) esto es un no-op completo: ni se inicializa el SDK ni se manda
// nada por red. Ver "Variables de entorno" en el README para cómo activarlo.
const dsn = import.meta.env.VITE_SENTRY_DSN;

// Datos personales de pacientes (DNI, email, teléfono) nunca deberían viajar en un reporte
// de error técnico — la política de privacidad promete esto explícitamente. Como defensa
// en profundidad, se los redacta acá aunque ya no deberían aparecer en los mensajes de error.
const PII_PATTERNS = [
  { regex: /\b\d{7,9}\b/g, replacement: '[dni-redactado]' },
  { regex: /[\w.+-]+@[\w-]+\.[\w.-]+/g, replacement: '[email-redactado]' },
  { regex: /\b(?:\+?54)?\s?9?\s?\d{2,4}[\s-]?\d{3,4}[\s-]?\d{3,4}\b/g, replacement: '[telefono-redactado]' },
];

function redactarPii(texto) {
  if (typeof texto !== 'string') return texto;
  return PII_PATTERNS.reduce((acc, { regex, replacement }) => acc.replace(regex, replacement), texto);
}

function redactarEvento(event) {
  if (event.message) event.message = redactarPii(event.message);
  event.exception?.values?.forEach((v) => {
    if (v.value) v.value = redactarPii(v.value);
  });
  if (event.extra) {
    for (const key of Object.keys(event.extra)) {
      event.extra[key] = redactarPii(event.extra[key]);
    }
  }
  return event;
}

export function initErrorTracking() {
  if (!dsn) return;
  Sentry.init({ dsn, tracesSampleRate: 0, beforeSend: redactarEvento });
}

export function reportError(error, extra) {
  if (!dsn) return;
  Sentry.captureException(error, extra ? { extra } : undefined);
}
