using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Hosting.Server;
using Microsoft.AspNetCore.Hosting.Server.Features;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;
using MyCompany.MyShop.Backend.Core.Entities;
using MyCompany.MyShop.Backend.Data;
using PactNet.Infrastructure.Outputters;
using PactNet.Verifier;
using MyCompany.MyShop.Backend.Tests.Support;
using System.Net;
using System.Text.Json;
using Testcontainers.PostgreSql;
using WireMock.RequestBuilders;
using WireMock.ResponseBuilders;
using WireMock.Server;
using Xunit;
using Xunit.Abstractions;

namespace MyCompany.MyShop.Backend.Tests.Contract;

/// <summary>
/// Replays the frontend consumer contract against the in-process provider, with external systems
/// (ERP / Tax / Clock) stubbed by in-process WireMock and provider states seeded into a
/// Testcontainers-managed Postgres. Mirrors the Java provider-verification harness: real Postgres
/// (so numeric / timestamptz semantics match the contract), in-process external stubs. Fails the
/// build if the backend drifts from the contract.
///
/// <para>The provider is hosted on a real Kestrel TCP port (not the in-memory <c>TestServer</c>):
/// PactNet's verifier is the native Rust <c>libpact_ffi</c>, which sends real HTTP over a socket,
/// so it cannot reach an in-memory test server. This mirrors the Java harness, which boots the app
/// with <c>webEnvironment = RANDOM_PORT</c> for the same reason.</para>
///
/// <para>The external-system URLs and <c>EXTERNAL_SYSTEM_MODE=stub</c> are passed as process
/// environment variables because that is how the gateways read them (env var first, config
/// fallback second). <c>stub</c> mode is required so <c>ClockGateway</c> calls the WireMock clock
/// instead of returning <c>DateTime.UtcNow</c> — the contract's time-sensitive states (e.g. the
/// New Year blackout) depend on a controllable clock. This mirrors the Java harness's
/// <c>external.system-mode=stub</c> + <c>erp.url</c>/<c>tax.url</c>/<c>clock.url</c> properties.
/// The Pact suite runs in its own <c>dotnet test</c> process, so mutating env vars is isolated;
/// they are restored on dispose regardless.</para>
///
/// <para>Test-layer separation is by namespace (the .NET equivalent of Java's source sets):
/// suites select with <c>--filter "FullyQualifiedName~...Tests.Contract"</c>.</para>
/// </summary>
public class BackendPactVerificationTest : IAsyncLifetime
{
    private readonly ITestOutputHelper _output;

    private readonly PostgreSqlContainer _postgres = new PostgreSqlBuilder()
        .WithImage("postgres:16-alpine")
        .WithDatabase("app")
        .WithUsername("app")
        .WithPassword("app")
        .Build();

    private WireMockServer _erp = null!;
    private WireMockServer _tax = null!;
    private WireMockServer _clock = null!;
    private KestrelWebApplicationFactory _factory = null!;
    private HttpListener _stateListener = null!;
    private Thread _stateThread = null!;
    private int _statePort;
    private AppDbContext _db = null!;
    private bool _running = true;

    private readonly Dictionary<string, string?> _savedEnv = new();

    public BackendPactVerificationTest(ITestOutputHelper output)
    {
        _output = output;
    }

    public async Task InitializeAsync()
    {
        await _postgres.StartAsync();

        _erp = WireMockServer.Start();
        _tax = WireMockServer.Start();
        _clock = WireMockServer.Start();

        // The gateways read these from environment variables first (config is only a fallback), so
        // setting them via WebApplicationFactory config alone would not take effect. stub mode makes
        // ClockGateway call the WireMock clock rather than DateTime.UtcNow.
        SetEnv("EXTERNAL_SYSTEM_MODE", "stub");
        SetEnv("ERP_API_URL", _erp.Url!);
        SetEnv("TAX_API_URL", _tax.Url!);
        SetEnv("CLOCK_API_URL", _clock.Url!);

        _factory = new KestrelWebApplicationFactory(_postgres.GetConnectionString());

        // Resolve a scoped AppDbContext from the test factory and create the schema.
        var scope = _factory.Services.CreateScope();
        _db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
        await _db.Database.EnsureCreatedAsync();

        // Start a standalone HttpListener to serve /_pact/provider-states.
        _statePort = FindFreePort();
        _stateListener = new HttpListener();
        _stateListener.Prefixes.Add($"http://127.0.0.1:{_statePort}/");
        _stateListener.Start();

        _stateThread = new Thread(ServeProviderStates) { IsBackground = true };
        _stateThread.Start();
    }

    [Fact]
    public void Verify()
    {
        var pactPath = Path.GetFullPath(
            Path.Combine(AppContext.BaseDirectory,
                "../../../../../../../contracts/frontend-backend.json"));

        var config = new PactVerifierConfig
        {
            Outputters = new List<IOutput> { new XUnitOutput(_output) },
        };

        new PactVerifier("backend", config)
            .WithHttpEndpoint(new Uri(_factory.ServerAddress))
            .WithFileSource(new FileInfo(pactPath))
            .WithCustomHeader("Authorization", "Bearer " + TestAuth.AdminAndCustomerToken)
            .WithProviderStateUrl(new Uri($"http://127.0.0.1:{_statePort}/"))
            .Verify();
    }

    public async Task DisposeAsync()
    {
        _running = false;
        _stateListener.Stop();
        await _db.DisposeAsync();
        _factory.Dispose();
        _erp.Stop();
        _tax.Stop();
        _clock.Stop();
        await _postgres.DisposeAsync();
        RestoreEnv();
    }

    private void SetEnv(string key, string value)
    {
        _savedEnv[key] = Environment.GetEnvironmentVariable(key);
        Environment.SetEnvironmentVariable(key, value);
    }

    private void RestoreEnv()
    {
        foreach (var (key, value) in _savedEnv)
            Environment.SetEnvironmentVariable(key, value);
    }

    // --- Provider-states HTTP listener ---

    private void ServeProviderStates()
    {
        while (_running)
        {
            HttpListenerContext ctx;
            try { ctx = _stateListener.GetContext(); }
            catch (HttpListenerException) { break; }
            catch (ObjectDisposedException) { break; }

            using var body = new StreamReader(ctx.Request.InputStream);
            var json = body.ReadToEnd();
            var doc = JsonDocument.Parse(json);
            var state = doc.RootElement.GetProperty("state").GetString() ?? string.Empty;
            var action = doc.RootElement.TryGetProperty("action", out var a) ? a.GetString() : "setup";

            if (action == "setup")
            {
                ResetState();
                ApplyState(state);
            }

            ctx.Response.StatusCode = 200;
            ctx.Response.Close();
        }
    }

    private void ResetState()
    {
        _erp.Reset();
        _tax.Reset();
        _clock.Reset();
        _db.Orders.RemoveRange(_db.Orders);
        _db.Coupons.RemoveRange(_db.Coupons);
        _db.SaveChanges();
    }

    private void ApplyState(string state)
    {
        switch (state)
        {
            case "product BOOK-123 exists and US is taxable":
                StubClock("2026-03-10T12:00:00Z");
                StubProduct("BOOK-123", "10.00");
                StubPromotion(false, "1.0");
                StubTax("US", "0.10");
                break;

            case "order placement is blocked by the New Year blackout":
                StubClock("2026-12-31T23:59:00Z");
                break;

            // Everything the order needs is in place — the coupon is the one thing missing, so the
            // rejection the frontend renders is the coupon's and not something else failing first.
            // ResetState empties the coupon table, so nothing has to be un-seeded.
            case "coupon INVALIDCOUPON does not exist":
                StubClock("2026-03-10T12:00:00Z");
                StubProduct("BOOK-123", "10.00");
                StubPromotion(false, "1.0");
                StubTax("US", "0.10");
                break;

            // ERP is the one that says a SKU is not a product. The clock still has to answer: it is
            // consulted before the product is looked up.
            case "product NON-EXISTENT-SKU-12345 does not exist":
                StubClock("2026-03-10T12:00:00Z");
                StubNoProduct("NON-EXISTENT-SKU-12345");
                break;

            // The product must resolve for the country to be the thing that fails — Tax is consulted
            // only after ERP has answered.
            case "product BOOK-123 exists and XX is not taxable":
                StubClock("2026-03-10T12:00:00Z");
                StubProduct("BOOK-123", "10.00");
                StubPromotion(false, "1.0");
                StubNoTax("XX");
                break;

            // 22:15 on December 31st — inside the cancellation blackout, with a cancellable order to
            // aim at.
            case "order cancellation is blocked by the New Year blackout":
                StubClock("2026-12-31T22:15:00Z");
                _db.Orders.Add(SampleOrder("ORD-1"));
                _db.SaveChanges();
                break;

            case "at least one order exists":
                _db.Orders.Add(SampleOrder("ORD-HIST-1"));
                _db.SaveChanges();
                break;

            // Shared by viewing ORD-1 and by cancelling it. Cancelling asks the clock before it
            // touches the order (the blackout is decided first), so the clock is stubbed here too —
            // at a date well clear of the blackout, which is what makes the cancellation succeed.
            case "order ORD-1 is placed":
                StubClock("2026-03-10T12:00:00Z");
                _db.Orders.Add(SampleOrder("ORD-1"));
                _db.SaveChanges();
                break;

            case "order ORD-1 is cancelled":
                _db.Orders.Add(SampleOrder("ORD-1", OrderStatus.CANCELLED));
                _db.SaveChanges();
                break;

            case "order ORD-1 is delivered":
                _db.Orders.Add(SampleOrder("ORD-1", OrderStatus.DELIVERED));
                _db.SaveChanges();
                break;

            case "no order UNKNOWN exists":
                break; // cleared in ResetState

            case "at least one coupon exists":
                _db.Coupons.Add(new Coupon
                {
                    Code = "SAVE10",
                    DiscountRate = 0.20m,
                    UsageLimit = 100,
                    UsedCount = 0,
                });
                _db.SaveChanges();
                break;

            case "coupon SAVE10 exists":
                StubClock("2026-03-10T12:00:00Z");
                StubProduct("BOOK-123", "10.00");
                StubPromotion(false, "1.0");
                StubTax("US", "0.10");
                _db.Coupons.Add(new Coupon
                {
                    Code = "SAVE10",
                    DiscountRate = 0.20m,
                    UsageLimit = 100,
                    UsedCount = 0,
                });
                _db.SaveChanges();
                break;

            case "no coupon SAVE10 exists yet":
                break; // cleared in ResetState
        }
    }

    private void StubClock(string isoInstant) =>
        _clock.Given(Request.Create().WithPath("/api/time").UsingGet())
              .RespondWith(Response.Create().WithStatusCode(200)
                  .WithBody($"{{\"time\":\"{isoInstant}\"}}")
                  .WithHeader("Content-Type", "application/json"));

    private void StubProduct(string sku, string price) =>
        _erp.Given(Request.Create().WithPath($"/api/products/{sku}").UsingGet())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithBody($"{{\"id\":\"{sku}\",\"price\":{price}}}")
                .WithHeader("Content-Type", "application/json"));

    private void StubPromotion(bool active, string discount) =>
        _erp.Given(Request.Create().WithPath("/api/promotion").UsingGet())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithBody($"{{\"promotionActive\":{active.ToString().ToLower()},\"discount\":{discount}}}")
                .WithHeader("Content-Type", "application/json"));

    // An unknown product / an untaxed country, programmed as deliberately as a known one: both
    // external systems answer 404, which is what the gateways turn into the rejection.
    private void StubNoProduct(string sku) =>
        _erp.Given(Request.Create().WithPath($"/api/products/{sku}").UsingGet())
            .RespondWith(Response.Create().WithStatusCode(404));

    private void StubNoTax(string country) =>
        _tax.Given(Request.Create().WithPath($"/api/countries/{country}").UsingGet())
            .RespondWith(Response.Create().WithStatusCode(404));

    private void StubTax(string country, string rate) =>
        _tax.Given(Request.Create().WithPath($"/api/countries/{country}").UsingGet())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithBody($"{{\"id\":\"{country}\",\"countryName\":\"{country}\",\"taxRate\":{rate}}}")
                .WithHeader("Content-Type", "application/json"));

    private static Order SampleOrder(string orderNumber) => SampleOrder(orderNumber, OrderStatus.PLACED);

    private static Order SampleOrder(string orderNumber, OrderStatus status) => new()
    {
        OrderNumber = orderNumber,
        OrderTimestamp = DateTime.Parse("2026-03-10T12:00:00Z").ToUniversalTime(),
        Country = "US",
        Sku = "BOOK-123",
        Quantity = 2,
        UnitPrice = 10.00m,
        BasePrice = 20.00m,
        DiscountRate = 0m,
        DiscountAmount = 0m,
        SubtotalPrice = 20.00m,
        TaxRate = 0.10m,
        TaxAmount = 2.00m,
        TotalPrice = 22.00m,
        Status = status,
        AppliedCouponCode = null,
    };

    private static int FindFreePort()
    {
        var listener = new System.Net.Sockets.TcpListener(IPAddress.Loopback, 0);
        listener.Start();
        var port = ((IPEndPoint)listener.LocalEndpoint).Port;
        listener.Stop();
        return port;
    }

    /// <summary>
    /// Hosts the provider on a real Kestrel server bound to a free loopback port, so PactNet's
    /// native verifier can reach it over a real socket. The default <see cref="WebApplicationFactory{TEntryPoint}"/>
    /// uses an in-memory <c>TestServer</c> with no listening port, which the native verifier cannot
    /// reach. Follows the documented ASP.NET Core "test with a real Kestrel server" pattern: build
    /// the TestServer host, then re-build the same configured host using Kestrel and start it.
    /// </summary>
    private sealed class KestrelWebApplicationFactory : WebApplicationFactory<Program>
    {
        private readonly string _dbConnectionString;
        private IHost? _kestrelHost;

        public KestrelWebApplicationFactory(string dbConnectionString)
        {
            _dbConnectionString = dbConnectionString;
        }

        /// <summary>The real <c>http://127.0.0.1:&lt;port&gt;</c> address Kestrel bound to.</summary>
        public string ServerAddress
        {
            get
            {
                EnsureServer();
                return ClientOptions.BaseAddress.ToString();
            }
        }

        private void EnsureServer()
        {
            if (_kestrelHost is null)
                using (CreateDefaultClient()) { }
        }

        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.UseTestAuth();
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(
                    d => d.ServiceType == typeof(DbContextOptions<AppDbContext>));
                if (descriptor != null)
                    services.Remove(descriptor);

                services.AddDbContext<AppDbContext>(options =>
                    options.UseNpgsql(_dbConnectionString));
            });
        }

        protected override IHost CreateHost(IHostBuilder builder)
        {
            // Build the TestServer host first, before switching the builder to Kestrel.
            var testHost = builder.Build();

            // Re-build the same configured host on Kestrel, bound to a free loopback port.
            builder.ConfigureWebHost(webHostBuilder =>
                webHostBuilder.UseKestrel().UseUrls("http://127.0.0.1:0"));
            _kestrelHost = builder.Build();
            _kestrelHost.Start();

            // Publish the dynamically-assigned Kestrel address so ServerAddress / clients use it.
            // Replace any all-interfaces binding (0.0.0.0) with loopback: 0.0.0.0 is not a
            // valid connection target on Windows, causing the PactNet verifier to fail.
            var addresses = _kestrelHost.Services.GetRequiredService<IServer>()
                .Features.Get<IServerAddressesFeature>();
            ClientOptions.BaseAddress = addresses!.Addresses
                .Select(a => new Uri(a.Replace("://0.0.0.0:", "://127.0.0.1:", StringComparison.Ordinal)))
                .Last();

            testHost.Start();
            return testHost;
        }

        protected override void Dispose(bool disposing)
        {
            base.Dispose(disposing);
            if (disposing)
                _kestrelHost?.Dispose();
        }
    }

    /// <summary>Routes the Pact native verifier output to xUnit's test output.</summary>
    private sealed class XUnitOutput : IOutput
    {
        private readonly ITestOutputHelper _output;

        public XUnitOutput(ITestOutputHelper output) => _output = output;

        public void WriteLine(string line) => _output.WriteLine(line);
    }
}
