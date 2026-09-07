import * as Sentry from '@sentry/react';

// Sin VITE_SENTRY_DSN configurada (el default en desarrollo y hasta que se cree una
// cuenta real de Sentry) esto es un no-op completo: ni se inicializa el SDK ni se manda
// nada por red. Ver "Variables de entorno" en el README para cómo activarlo.
const dsn = import.meta.env.VITE_SENTRY_DSN;

export function initErrorTracking() {
  if (!dsn) return;
  Sentry.init({ dsn, tracesSampleRate: 0 });
}

export function reportError(error, extra) {
  if (!dsn) return;
  Sentry.captureException(error, extra ? { extra } : undefined);
}
