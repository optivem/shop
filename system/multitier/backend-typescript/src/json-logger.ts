import { ConsoleLogger, type LogLevel } from '@nestjs/common';
import { logs, SeverityNumber, trace } from '@opentelemetry/api';

// Maps Nest levels onto OTel severities for the OTLP log export.
const SEVERITY: Record<LogLevel, SeverityNumber> = {
  verbose: SeverityNumber.TRACE,
  debug: SeverityNumber.DEBUG,
  log: SeverityNumber.INFO,
  warn: SeverityNumber.WARN,
  error: SeverityNumber.ERROR,
  fatal: SeverityNumber.FATAL,
};

interface JsonLogOptions {
  context: string;
  logLevel: LogLevel;
  writeStreamType?: 'stdout' | 'stderr';
  errorStack?: unknown;
}

/**
 * Nest's built-in JSON console logger, one line per entry on stdout, with the active span's trace_id and
 * span_id added so logs correlate with traces. Outside a span (or with tracing off) the two fields are absent.
 * Each entry is also emitted to the OTel logs API, which exports it over OTLP when tracing.ts started the SDK
 * and is a no-op otherwise.
 */
export class JsonLogger extends ConsoleLogger {
  constructor() {
    super({ json: true });
  }

  protected override printAsJson(
    message: unknown,
    options: JsonLogOptions,
  ): void {
    super.printAsJson(message, { ...options, writeStreamType: 'stdout' });
    logs.getLogger('myshop').emit({
      severityNumber: SEVERITY[options.logLevel],
      severityText: options.logLevel.toUpperCase(),
      body: typeof message === 'string' ? message : JSON.stringify(message),
      attributes: {
        ...(options.context ? { 'log.context': options.context } : {}),
        ...(typeof options.errorStack === 'string'
          ? { 'exception.stacktrace': options.errorStack }
          : {}),
      },
    });
  }

  protected override getJsonLogObject(
    message: unknown,
    options: JsonLogOptions,
  ) {
    const logObject: {
      level: LogLevel;
      pid: number;
      timestamp: number;
      message: unknown;
      context?: string;
      stack?: unknown;
      trace_id?: string;
      span_id?: string;
    } = super.getJsonLogObject(message, options);
    const spanContext = trace.getActiveSpan()?.spanContext();
    if (spanContext !== undefined) {
      logObject.trace_id = spanContext.traceId;
      logObject.span_id = spanContext.spanId;
    }
    return logObject;
  }
}
