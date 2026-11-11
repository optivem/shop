using System.Net;
using Microsoft.Extensions.Http.Resilience;
using MyCompany.MyShop.Monolith.Core.Services.External;
using Polly;

namespace MyCompany.MyShop.Monolith.Api.Resilience;

/// <summary>
/// Outbound calls to the ERP, Clock and Tax systems go through typed HttpClients wrapped in a
/// resilience pipeline: retry (idempotent GETs only) → circuit breaker (one per external system)
/// → per-attempt timeout. Values come from <c>Resilience:{Erp|Clock|Tax}:*</c> in appsettings.
/// There is deliberately no fallback: a guessed price would be a money-correctness bug.
/// </summary>
public static class ResilienceExtensions
{
    public static IServiceCollection AddShopExternalSystemClients(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddHttpClient<ErpGateway>().AddShopResilience("Erp", configuration);
        services.AddHttpClient<ClockGateway>().AddShopResilience("Clock", configuration);
        services.AddHttpClient<TaxGateway>().AddShopResilience("Tax", configuration);
        return services;
    }

    public static IHttpClientBuilder AddShopResilience(this IHttpClientBuilder builder, string externalSystem, IConfiguration configuration)
    {
        var settings = new ResilienceSettings();
        configuration.GetSection($"Resilience:{externalSystem}").Bind(settings);

        builder.ConfigurePrimaryHttpMessageHandler(() => new SocketsHttpHandler
        {
            ConnectTimeout = TimeSpan.FromMilliseconds(settings.ConnectTimeoutMilliseconds)
        });

        builder.AddResilienceHandler(externalSystem, pipeline =>
        {
            // MaxAttempts counts the first call, Polly counts retries only; MaxAttempts 1 disables retry.
            if (settings.MaxAttempts > 1)
            {
                pipeline.AddRetry(new HttpRetryStrategyOptions
                {
                    MaxRetryAttempts = settings.MaxAttempts - 1,
                    BackoffType = DelayBackoffType.Exponential,
                    UseJitter = true,
                    Delay = TimeSpan.FromMilliseconds(settings.RetryBaseDelayMilliseconds),
                    // Never retry POST/PUT/PATCH/DELETE: a replayed non-idempotent call can apply twice.
                    ShouldHandle = args => ValueTask.FromResult(
                        IsIdempotent(args.Context.GetRequestMessage()) && IsFailure(args.Outcome))
                });
            }

            pipeline
                .AddCircuitBreaker(new HttpCircuitBreakerStrategyOptions
                {
                    FailureRatio = settings.FailureRatio,
                    MinimumThroughput = settings.MinimumThroughput,
                    SamplingDuration = TimeSpan.FromSeconds(settings.SamplingDurationSeconds),
                    BreakDuration = TimeSpan.FromSeconds(settings.BreakDurationSeconds),
                    ShouldHandle = args => ValueTask.FromResult(IsFailure(args.Outcome))
                })
                .AddTimeout(TimeSpan.FromMilliseconds(settings.AttemptTimeoutMilliseconds));
        });

        return builder;
    }

    private static bool IsIdempotent(HttpRequestMessage? request) =>
        request?.Method == HttpMethod.Get || request?.Method == HttpMethod.Head;

    // Timeouts, connection errors and 5xx count as failures; 4xx (e.g. 404 product-not-found) are answers.
    private static bool IsFailure(Outcome<HttpResponseMessage> outcome) =>
        outcome.Exception is HttpRequestException or Polly.Timeout.TimeoutRejectedException
        || (outcome.Result is { } response && (int)response.StatusCode >= (int)HttpStatusCode.InternalServerError);

    private sealed class ResilienceSettings
    {
        public int AttemptTimeoutMilliseconds { get; set; } = 2000;
        public int ConnectTimeoutMilliseconds { get; set; } = 1000;
        public int MaxAttempts { get; set; } = 3;
        public int RetryBaseDelayMilliseconds { get; set; } = 200;
        public double FailureRatio { get; set; } = 0.5;
        public int MinimumThroughput { get; set; } = 5;
        public int SamplingDurationSeconds { get; set; } = 30;
        public int BreakDurationSeconds { get; set; } = 10;
    }
}
