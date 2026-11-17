using Microsoft.EntityFrameworkCore;
using MyCompany.MyShop.Monolith.Data;

namespace MyCompany.MyShop.Monolith.Core.Services;

/// <summary>
/// Deletes <c>idempotency_keys</c> rows older than the 24 h TTL on a fixed interval (<c>Idempotency:PurgeIntervalMinutes</c>,
/// default 60; the first run waits one interval). Expired rows are already ignored and replaced on their next use; this only
/// keeps the table from growing without bound. Every instance runs it; the DELETE is idempotent, so overlapping runs are harmless.
/// </summary>
public sealed class IdempotencyKeyPurgeService(
    IServiceScopeFactory scopeFactory,
    IConfiguration configuration,
    ILogger<IdempotencyKeyPurgeService> logger) : BackgroundService
{
    private const int DefaultPurgeIntervalMinutes = 60;

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        var minutes = configuration.GetValue("Idempotency:PurgeIntervalMinutes", DefaultPurgeIntervalMinutes);
        using var timer = new PeriodicTimer(TimeSpan.FromMinutes(minutes));
        while (await timer.WaitForNextTickAsync(stoppingToken))
        {
            try
            {
                var deleted = await PurgeExpiredAsync(stoppingToken);
                if (deleted > 0)
                {
                    logger.LogInformation("Purged {Deleted} expired idempotency keys", deleted);
                }
            }
            catch (Exception ex) when (ex is not OperationCanceledException)
            {
                logger.LogWarning(ex, "Purging expired idempotency keys failed; retrying next interval");
            }
        }
    }

    private async Task<int> PurgeExpiredAsync(CancellationToken cancellationToken)
    {
        using var scope = scopeFactory.CreateScope();
        var dbContext = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        var cutoff = DateTime.UtcNow - OrderService.IdempotencyKeyTtl;
        return await dbContext.IdempotencyKeys
            .Where(k => k.CreatedAt <= cutoff)
            .ExecuteDeleteAsync(cancellationToken);
    }
}
