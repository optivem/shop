// Next.js instrumentation hook (runs once per server start, before any request is handled).
// OpenTelemetry is opt-in: the SDK loads only when OTEL_EXPORTER_OTLP_ENDPOINT is set and non-empty, so by default
// nothing is imported or started and the app behaves exactly as before.
export async function register(): Promise<void> {
  const endpoint = process.env.OTEL_EXPORTER_OTLP_ENDPOINT;
  if (process.env.NEXT_RUNTIME === 'nodejs' && endpoint !== undefined && endpoint.trim() !== '') {
    await import('./instrumentation-node');
  }
  if (process.env.NEXT_RUNTIME === 'nodejs') {
    // Hourly purge of expired idempotency keys (node runtime only, since it needs the pg pool), after the
    // OpenTelemetry SDK so its pg instrumentation is in place first.
    const { startIdempotencyKeyPurge } = await import('./lib/idempotency-purge');
    startIdempotencyKeyPurge();
  }
}
