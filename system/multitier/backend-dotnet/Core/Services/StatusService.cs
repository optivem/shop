using MyCompany.MyShop.Backend.Data;

namespace MyCompany.MyShop.Backend.Core.Services;

/// <summary>
/// Public reachability probes behind the storefront's first-load "waking up" dialog. Probing also wakes the
/// scale-to-zero external systems early. Outbound calls use a plain HttpClient (no retry, no circuit breaker)
/// so a probe cannot trip the breaker real traffic uses; any failure is just "down".
/// </summary>
public class StatusService
{
    private static readonly TimeSpan ProbeTimeout = TimeSpan.FromSeconds(10);

    private readonly AppDbContext _dbContext;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly string _externalSystemMode;
    private readonly string _erpUrl;
    private readonly string _taxUrl;
    private readonly string _clockUrl;

    public StatusService(AppDbContext dbContext, IHttpClientFactory httpClientFactory, IConfiguration configuration)
    {
        _dbContext = dbContext;
        _httpClientFactory = httpClientFactory;
        _externalSystemMode = Environment.GetEnvironmentVariable("EXTERNAL_SYSTEM_MODE") ?? configuration["External:SystemMode"] ?? "real";
        _erpUrl = Environment.GetEnvironmentVariable("ERP_API_URL") ?? configuration["Erp:Url"] ?? "http://localhost:9001/erp";
        _taxUrl = Environment.GetEnvironmentVariable("TAX_API_URL") ?? configuration["Tax:Url"] ?? "http://localhost:9001/tax";
        _clockUrl = Environment.GetEnvironmentVariable("CLOCK_API_URL") ?? configuration["Clock:Url"] ?? "http://localhost:9001/clock";
    }

    /// <summary>True when the system is up, false when down, null when the name is not a known system.</summary>
    public async Task<bool?> IsUpAsync(string system, CancellationToken cancellationToken) => system switch
    {
        "backend" => await DatabaseAnswersAsync(cancellationToken),
        "erp" => await ProbeAsync(_erpUrl, cancellationToken),
        "tax" => await ProbeAsync(_taxUrl, cancellationToken),
        // Real mode reads the system clock, so there is nothing to wake or reach.
        "clock" => _externalSystemMode == "real" || await ProbeAsync(_clockUrl, cancellationToken),
        _ => null,
    };

    private async Task<bool> DatabaseAnswersAsync(CancellationToken cancellationToken)
    {
        try
        {
            return await _dbContext.Database.CanConnectAsync(cancellationToken);
        }
        catch (System.Exception)
        {
            return false;
        }
    }

    private async Task<bool> ProbeAsync(string baseUrl, CancellationToken cancellationToken)
    {
        try
        {
            using var client = _httpClientFactory.CreateClient();
            client.Timeout = ProbeTimeout;
            using var response = await client.GetAsync($"{baseUrl}/health", cancellationToken);
            return response.IsSuccessStatusCode;
        }
        catch (System.Exception)
        {
            return false;
        }
    }
}
