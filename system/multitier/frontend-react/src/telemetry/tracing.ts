// Browser tracing, loaded by main.tsx through a dynamic import only when /config.js provides
// otelExporterOtlpEndpoint, so with tracing off none of this code is fetched or run.
//
// Each fetch() becomes a client span, and a W3C traceparent header is added to calls to the backend API, which
// continues the same trace through to ERP/Clock. The API is same-origin (/api/ is proxied by nginx, or by Vite in
// dev); propagateTraceHeaderCorsUrls names it explicitly so a cross-origin API base keeps working too.
//
// Spans are exported straight from the browser over OTLP/HTTP (JSON) to <endpoint>/v1/traces, so the collector
// must accept CORS requests from the storefront origin.
//
// No context manager (e.g. zone.js) is registered on purpose: it patches browser globals app-wide, and every
// fetch already starts its own trace that the backend joins.
import { propagation, trace } from '@opentelemetry/api';
import { W3CTraceContextPropagator } from '@opentelemetry/core';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import { resourceFromAttributes } from '@opentelemetry/resources';
import { BatchSpanProcessor, TracerProvider } from '@opentelemetry/sdk-trace';

const SERVICE_NAME = 'myshop-frontend-react';

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
}

export function startTracing(otlpEndpoint: string): void {
  const tracesUrl = `${otlpEndpoint.replace(/\/+$/, '')}/v1/traces`;
  const provider = new TracerProvider({
    resource: resourceFromAttributes({ 'service.name': SERVICE_NAME }),
    spanProcessors: [new BatchSpanProcessor({ exporter: new OTLPTraceExporter({ url: tracesUrl }) })],
  });
  trace.setGlobalTracerProvider(provider);
  propagation.setGlobalPropagator(new W3CTraceContextPropagator());

  registerInstrumentations({
    tracerProvider: provider,
    instrumentations: [
      new FetchInstrumentation({
        propagateTraceHeaderCorsUrls: [new RegExp(`^${escapeRegExp(globalThis.location.origin)}/api/`)],
        ignoreUrls: [tracesUrl],
      }),
    ],
  });
}
