// OpenTelemetry bootstrap. Imported first by main.ts so the instrumentations patch http/express/pg before
// Nest loads them. Opt-in: the SDK starts only when OTEL_EXPORTER_OTLP_ENDPOINT is set and non-empty; when it
// is unset the app runs exactly as before (no exporter, no connection attempts).
import { OTLPLogExporter } from '@opentelemetry/exporter-logs-otlp-proto';
import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-proto';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-proto';
import { ExpressInstrumentation } from '@opentelemetry/instrumentation-express';
import { HttpInstrumentation } from '@opentelemetry/instrumentation-http';
import { NestInstrumentation } from '@opentelemetry/instrumentation-nestjs-core';
import { PgInstrumentation } from '@opentelemetry/instrumentation-pg';
import { UndiciInstrumentation } from '@opentelemetry/instrumentation-undici';
import { BatchLogRecordProcessor } from '@opentelemetry/sdk-logs';
import { PeriodicExportingMetricReader } from '@opentelemetry/sdk-metrics';
import { NodeSDK } from '@opentelemetry/sdk-node';

const DEFAULT_SERVICE_NAME = 'myshop-backend-typescript';

function nonEmpty(value: string | undefined): string | undefined {
  return value !== undefined && value.trim() !== '' ? value : undefined;
}

if (nonEmpty(process.env.OTEL_EXPORTER_OTLP_ENDPOINT) !== undefined) {
  // OTLP/HTTP (protobuf) exporters; they read OTEL_EXPORTER_OTLP_ENDPOINT themselves and append /v1/traces,
  // /v1/metrics, /v1/logs. Logs come from JsonLogger, which mirrors each stdout line to the OTel logs API.
  const sdk = new NodeSDK({
    serviceName:
      nonEmpty(process.env.OTEL_SERVICE_NAME) ?? DEFAULT_SERVICE_NAME,
    traceExporter: new OTLPTraceExporter(),
    metricReaders: [
      new PeriodicExportingMetricReader({ exporter: new OTLPMetricExporter() }),
    ],
    logRecordProcessors: [
      new BatchLogRecordProcessor({ exporter: new OTLPLogExporter() }),
    ],
    // Server spans (http/express/nest), DB spans (pg), and client spans + W3C traceparent on the
    // outbound fetch calls to ERP/Clock/Tax (Node's fetch is undici).
    instrumentations: [
      new HttpInstrumentation(),
      new ExpressInstrumentation(),
      new NestInstrumentation(),
      new PgInstrumentation(),
      new UndiciInstrumentation(),
    ],
  });
  sdk.start();

  // Flush buffered spans/metrics on stop. A signal listener replaces Node's default exit, so exit explicitly
  // with the conventional 128+signal code.
  const shutdownOn = (signal: 'SIGTERM' | 'SIGINT', code: number) => {
    process.once(signal, () => {
      sdk
        .shutdown()
        .catch(() => undefined)
        .finally(() => process.exit(code));
    });
  };
  shutdownOn('SIGTERM', 143);
  shutdownOn('SIGINT', 130);
}
