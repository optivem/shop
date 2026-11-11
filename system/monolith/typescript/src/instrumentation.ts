// Next.js instrumentation hook (runs once per server start, before any request is handled).
// OpenTelemetry is opt-in: the SDK loads only when OTEL_EXPORTER_OTLP_ENDPOINT is set and non-empty, so by default
// nothing is imported or started and the app behaves exactly as before.
export async function register(): Promise<void> {
  const endpoint = process.env.OTEL_EXPORTER_OTLP_ENDPOINT;
  if (process.env.NEXT_RUNTIME === 'nodejs' && endpoint !== undefined && endpoint.trim() !== '') {
    await import('./instrumentation-node');
  }
}
