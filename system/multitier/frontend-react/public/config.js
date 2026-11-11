// Runtime configuration. In the container, nginx serves this path with values from the
// KEYCLOAK_* and OTEL_BROWSER_EXPORTER_OTLP_ENDPOINT environment variables; this static copy is used by the
// Vite dev server (add otelExporterOtlpEndpoint: 'http://localhost:4318' here to trace locally).
window.__APP_CONFIG__ = {};
