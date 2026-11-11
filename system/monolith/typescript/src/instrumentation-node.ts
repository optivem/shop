// OpenTelemetry Node SDK, loaded by instrumentation.ts only when OTLP export is configured. Next.js creates the
// server spans for routes itself (continuing an incoming W3C traceparent); this adds the exporters plus client
// spans and traceparent propagation for the outbound fetch calls to ERP/Clock/Tax (Node's fetch is undici) and
// DB spans for pg. The OTLP/HTTP (protobuf) exporters read OTEL_EXPORTER_OTLP_ENDPOINT and append /v1/traces,
// /v1/metrics, /v1/logs. Logs come from lib/logger.ts, which mirrors each stdout line to the OTel logs API.
import { OTLPLogExporter } from '@opentelemetry/exporter-logs-otlp-proto';
import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-proto';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-proto';
import { HttpInstrumentation } from '@opentelemetry/instrumentation-http';
import { PgInstrumentation } from '@opentelemetry/instrumentation-pg';
import { UndiciInstrumentation } from '@opentelemetry/instrumentation-undici';
import { BatchLogRecordProcessor } from '@opentelemetry/sdk-logs';
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
import { NodeSDK } from '@opentelemetry/sdk-node';

const DEFAULT_SERVICE_NAME = 'myshop-monolith-typescript';

const serviceName = process.env.OTEL_SERVICE_NAME?.trim();

const sdk = new NodeSDK({
  serviceName: serviceName !== undefined && serviceName !== '' ? serviceName : DEFAULT_SERVICE_NAME,
  traceExporter: new OTLPTraceExporter(),
  metricReaders: [new PeriodicExportingMetricReader({ exporter: new OTLPMetricExporter() })],
  logRecordProcessors: [new BatchLogRecordProcessor({ exporter: new OTLPLogExporter() })],
  instrumentations: [new HttpInstrumentation(), new UndiciInstrumentation(), new PgInstrumentation()],
});
sdk.start();

// Flush buffered telemetry on stop. A signal listener replaces Node's default exit, so exit explicitly with the
// conventional 128+signal code.
for (const [signal, code] of [['SIGTERM', 143], ['SIGINT', 130]] as const) {
  process.once(signal, () => {
    sdk
      .shutdown()
      .catch(() => undefined)
      .finally(() => process.exit(code));
  });
}
