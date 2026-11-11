import { logs, SeverityNumber, trace } from '@opentelemetry/api';

type Level = 'info' | 'warn' | 'error';

const SEVERITY: Record<Level, SeverityNumber> = {
  info: SeverityNumber.INFO,
  warn: SeverityNumber.WARN,
  error: SeverityNumber.ERROR,
};

function describeError(error: unknown): { error?: string; stack?: string } {
  if (error === undefined) return {};
  if (error instanceof Error) return { error: `${error.name}: ${error.message}`, stack: error.stack };
  return { error: typeof error === 'string' ? error : JSON.stringify(error) };
}

/**
 * Structured JSON log line on stdout, with the active span's trace_id and span_id so logs correlate with traces
 * (absent outside a span or when tracing is off). The entry is also emitted to the OTel logs API, which exports it
 * over OTLP when instrumentation-node.ts started the SDK and is a no-op otherwise.
 */
function write(level: Level, message: string, error?: unknown): void {
  const spanContext = trace.getActiveSpan()?.spanContext();
  const details = describeError(error);
  const entry = {
    level,
    timestamp: new Date().toISOString(),
    message,
    ...details,
    ...(spanContext ? { trace_id: spanContext.traceId, span_id: spanContext.spanId } : {}),
  };
  process.stdout.write(`${JSON.stringify(entry)}\n`);
  logs.getLogger('myshop').emit({
    severityNumber: SEVERITY[level],
    severityText: level.toUpperCase(),
    body: message,
    attributes: {
      ...(details.error !== undefined ? { 'exception.message': details.error } : {}),
      ...(details.stack !== undefined ? { 'exception.stacktrace': details.stack } : {}),
    },
  });
}

export const logger = {
  info: (message: string) => write('info', message),
  warn: (message: string, error?: unknown) => write('warn', message, error),
  error: (message: string, error?: unknown) => write('error', message, error),
};
