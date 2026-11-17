using Microsoft.AspNetCore.Diagnostics.HealthChecks;
using Microsoft.Extensions.Diagnostics.HealthChecks;
using MyCompany.MyShop.Backend.Data;

namespace MyCompany.MyShop.Backend.Api.Observability;

/// <summary>
/// Liveness and readiness probes, public and without component details (parity with the Java backends' actuator
/// probes): <c>/livez</c> answers while the process runs and checks no dependency; <c>/readyz</c> also needs the
/// database. Both return <c>{"status":"UP"}</c> (200) or <c>{"status":"DOWN"}</c> (503). The Docker HEALTHCHECK
/// probes <c>/livez</c>; <c>/health</c> stays as the system tests' probe.
/// </summary>
public static class HealthProbes
{
    public const string LivenessPath = "/livez";
    public const string ReadinessPath = "/readyz";
    private const string ReadyTag = "ready";

    public static IServiceCollection AddShopHealthProbes(this IServiceCollection services)
    {
        services.AddHealthChecks().AddCheck<DatabaseHealthCheck>("db", tags: [ReadyTag]);
        return services;
    }

    public static WebApplication MapShopHealthProbes(this WebApplication app)
    {
        app.MapHealthChecks(LivenessPath, Options(_ => false)).AllowAnonymous();
        app.MapHealthChecks(ReadinessPath, Options(check => check.Tags.Contains(ReadyTag))).AllowAnonymous();
        return app;
    }

    private static HealthCheckOptions Options(Func<HealthCheckRegistration, bool> predicate) => new()
    {
        Predicate = predicate,
        ResponseWriter = (context, report) =>
        {
            context.Response.ContentType = "application/json";
            var status = report.Status == HealthStatus.Healthy ? "UP" : "DOWN";
            return context.Response.WriteAsJsonAsync(new { status });
        },
    };

    private sealed class DatabaseHealthCheck(AppDbContext dbContext) : IHealthCheck
    {
        public async Task<HealthCheckResult> CheckHealthAsync(HealthCheckContext context, CancellationToken cancellationToken = default) =>
            await dbContext.Database.CanConnectAsync(cancellationToken)
                ? HealthCheckResult.Healthy()
                : HealthCheckResult.Unhealthy("The database is not reachable");
    }
}
