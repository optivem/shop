using System.Diagnostics;
using System.Text.Json;
using Microsoft.Extensions.Logging.Abstractions;
using Microsoft.Extensions.Logging.Console;
using OpenTelemetry.Exporter;
using OpenTelemetry.Logs;
using OpenTelemetry.Metrics;
using OpenTelemetry.Resources;
using OpenTelemetry.Trace;

namespace MyCompany.MyShop.Backend.Api.Observability;

public static class ObservabilityExtensions
{
    private const string DefaultServiceName = "myshop-backend-dotnet";

    /// <summary>
    /// Structured JSON logs to stdout (always on), each line carrying the current trace_id/span_id.
    /// OpenTelemetry traces, metrics and logs are exported over OTLP/HTTP only when OTEL_EXPORTER_OTLP_ENDPOINT
    /// is set; otherwise nothing is registered and the app behaves as before (no exporter, no connection attempts).
    /// </summary>
    public static WebApplicationBuilder AddShopObservability(this WebApplicationBuilder builder)
    {
        builder.Logging.AddConsole(options => options.FormatterName = TraceJsonConsoleFormatter.FormatterName);
        builder.Logging.AddConsoleFormatter<TraceJsonConsoleFormatter, ConsoleFormatterOptions>();

        var endpoint = Environment.GetEnvironmentVariable("OTEL_EXPORTER_OTLP_ENDPOINT");
        if (string.IsNullOrWhiteSpace(endpoint))
        {
            return builder;
        }

        var serviceName = Environment.GetEnvironmentVariable("OTEL_SERVICE_NAME");
        if (string.IsNullOrWhiteSpace(serviceName))
        {
            serviceName = DefaultServiceName;
        }

        // OTLP/HTTP unless OTEL_EXPORTER_OTLP_PROTOCOL says otherwise (the .NET exporter defaults to gRPC).
        var useHttp = string.IsNullOrWhiteSpace(Environment.GetEnvironmentVariable("OTEL_EXPORTER_OTLP_PROTOCOL"));
        void ConfigureOtlp(OtlpExporterOptions options)
        {
            if (useHttp)
            {
                options.Protocol = OtlpExportProtocol.HttpProtobuf;
            }
        }

        builder.Services.AddOpenTelemetry()
            .ConfigureResource(resource => resource.AddService(serviceName))
            .WithTracing(tracing => tracing
                .AddAspNetCoreInstrumentation()
                .AddHttpClientInstrumentation()
                .AddOtlpExporter(ConfigureOtlp))
            .WithMetrics(metrics => metrics
                .AddAspNetCoreInstrumentation()
                .AddHttpClientInstrumentation()
                .AddOtlpExporter(ConfigureOtlp));

        builder.Logging.AddOpenTelemetry(logging =>
        {
            logging.IncludeFormattedMessage = true;
            logging.AddOtlpExporter(ConfigureOtlp);
        });

        return builder;
    }
}

/// <summary>
/// One JSON object per line: timestamp, level, logger, message, trace_id, span_id (and exception when present).
/// trace_id/span_id are taken from the current Activity and omitted outside a span.
/// </summary>
public sealed class TraceJsonConsoleFormatter : ConsoleFormatter
{
    public const string FormatterName = "trace-json";

    public TraceJsonConsoleFormatter() : base(FormatterName)
    {
    }

    public override void Write<TState>(in LogEntry<TState> logEntry, IExternalScopeProvider? scopeProvider, TextWriter textWriter)
    {
        var message = logEntry.Formatter(logEntry.State, logEntry.Exception);
        if (message == null && logEntry.Exception == null)
        {
            return;
        }

        using var stream = new MemoryStream();
        using (var writer = new Utf8JsonWriter(stream))
        {
            writer.WriteStartObject();
            writer.WriteString("timestamp", DateTimeOffset.UtcNow.ToString("O"));
            writer.WriteString("level", logEntry.LogLevel.ToString());
            writer.WriteString("logger", logEntry.Category);
            writer.WriteString("message", message);

            var activity = Activity.Current;
            if (activity != null)
            {
                writer.WriteString("trace_id", activity.TraceId.ToHexString());
                writer.WriteString("span_id", activity.SpanId.ToHexString());
            }

            if (logEntry.Exception != null)
            {
                writer.WriteString("exception", logEntry.Exception.ToString());
            }

            writer.WriteEndObject();
        }

        textWriter.Write(System.Text.Encoding.UTF8.GetString(stream.GetBuffer(), 0, (int)stream.Length));
        textWriter.Write(Environment.NewLine);
    }
}
