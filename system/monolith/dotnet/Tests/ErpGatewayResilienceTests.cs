using System.Net;
using System.Text;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Monolith.Api.Resilience;
using MyCompany.MyShop.Monolith.Core.Exceptions;
using MyCompany.MyShop.Monolith.Core.Services.External;
using Polly.CircuitBreaker;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// The real ErpGateway behind the production resilience pipeline, with the ERP scripted at the HTTP
/// boundary: retries transient failures, leaves 404 alone, times out slow calls and fails fast once
/// the circuit breaker opens. Timings are shortened so the tests run in milliseconds.
/// </summary>
public class ErpGatewayResilienceTests
{
    private const string Book = """{"id":"BOOK-123","price":10.00}""";

    private static ErpGateway Gateway(ScriptedErpHandler erp, Dictionary<string, string?>? overrides = null)
    {
        var settings = new Dictionary<string, string?>
        {
            ["Erp:Url"] = "http://erp.test/erp",
            ["Resilience:Erp:AttemptTimeoutMilliseconds"] = "500",
            ["Resilience:Erp:MaxAttempts"] = "3",
            ["Resilience:Erp:RetryBaseDelayMilliseconds"] = "1",
            ["Resilience:Erp:MinimumThroughput"] = "100",
        };
        foreach (var (key, value) in overrides ?? new())
        {
            settings[key] = value;
        }

        var configuration = new ConfigurationBuilder().AddInMemoryCollection(settings).Build();
        var services = new ServiceCollection();
        services.AddLogging();
        services.AddSingleton<IConfiguration>(configuration);
        services.AddHttpClient<ErpGateway>()
            .AddShopResilience("Erp", configuration)
            .ConfigurePrimaryHttpMessageHandler(() => erp);
        return services.BuildServiceProvider().GetRequiredService<ErpGateway>();
    }

    [Fact]
    public async Task RetriesATransientFailureThenSucceeds()
    {
        var erp = new ScriptedErpHandler(HttpStatusCode.ServiceUnavailable, HttpStatusCode.OK);

        var result = await Gateway(erp).GetProductDetailsAsync("BOOK-123");

        Assert.NotNull(result);
        Assert.Equal(10.00m, result.Price);
        Assert.Equal(2, erp.Calls);
    }

    [Fact]
    public async Task DoesNotRetryProductNotFound()
    {
        var erp = new ScriptedErpHandler(HttpStatusCode.NotFound);

        var result = await Gateway(erp).GetProductDetailsAsync("UNKNOWN");

        Assert.Null(result);
        Assert.Equal(1, erp.Calls);
    }

    [Fact]
    public async Task ThrowsErpUnavailableOnceRetriesAreExhausted()
    {
        var erp = new ScriptedErpHandler(HttpStatusCode.InternalServerError);

        await Assert.ThrowsAsync<ErpUnavailableException>(() => Gateway(erp).GetProductDetailsAsync("BOOK-123"));

        Assert.Equal(3, erp.Calls);
    }

    [Fact]
    public async Task ThrowsErpUnavailableWhenTheErpIsTooSlow()
    {
        var erp = new ScriptedErpHandler(HttpStatusCode.OK) { Delay = TimeSpan.FromSeconds(10) };
        var gateway = Gateway(erp, new()
        {
            ["Resilience:Erp:AttemptTimeoutMilliseconds"] = "100",
            ["Resilience:Erp:MaxAttempts"] = "1",
        });

        var started = DateTime.UtcNow;
        await Assert.ThrowsAsync<ErpUnavailableException>(() => gateway.GetProductDetailsAsync("BOOK-123"));

        Assert.True(DateTime.UtcNow - started < TimeSpan.FromSeconds(5));
    }

    [Fact]
    public async Task FailsFastWithoutCallingTheErpOnceTheBreakerIsOpen()
    {
        var erp = new ScriptedErpHandler(HttpStatusCode.InternalServerError);
        var gateway = Gateway(erp, new()
        {
            ["Resilience:Erp:MaxAttempts"] = "1",
            ["Resilience:Erp:MinimumThroughput"] = "2",
        });
        await Assert.ThrowsAsync<ErpUnavailableException>(() => gateway.GetProductDetailsAsync("BOOK-123"));
        await Assert.ThrowsAsync<ErpUnavailableException>(() => gateway.GetProductDetailsAsync("BOOK-123"));

        var ex = await Assert.ThrowsAsync<ErpUnavailableException>(() => gateway.GetProductDetailsAsync("BOOK-123"));

        Assert.IsType<BrokenCircuitException>(ex.InnerException);
        Assert.Equal(2, erp.Calls);
    }

    /// <summary>Answers with the given statuses in order, repeating the last one; counts the calls it receives.</summary>
    private sealed class ScriptedErpHandler : HttpMessageHandler
    {
        private readonly HttpStatusCode[] _statuses;
        private int _calls;

        public ScriptedErpHandler(params HttpStatusCode[] statuses) => _statuses = statuses;

        public TimeSpan Delay { get; init; } = TimeSpan.Zero;

        public int Calls => _calls;

        protected override async Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            var call = Interlocked.Increment(ref _calls);
            if (Delay > TimeSpan.Zero)
            {
                await Task.Delay(Delay, cancellationToken);
            }

            var status = _statuses[Math.Min(call, _statuses.Length) - 1];
            return new HttpResponseMessage(status)
            {
                Content = new StringContent(status == HttpStatusCode.OK ? Book : "", Encoding.UTF8, "application/json")
            };
        }
    }
}
