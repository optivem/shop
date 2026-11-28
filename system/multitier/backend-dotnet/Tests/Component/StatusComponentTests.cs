using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.EntityFrameworkCore.Infrastructure;
using System.Net;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Backend.Data;
using MyCompany.MyShop.Backend.Tests.Support;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Component;

/// <summary>
/// The public /api/status/{system} probes over HTTP: no token needed, exactly {"status":"UP"} (200) or
/// {"status":"DOWN"} (503), 404 for an unknown system, and the outbound probe hits {url}/health.
/// </summary>
public class StatusComponentTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _baseFactory;
    private readonly List<string> _calls = [];

    public StatusComponentTests(WebApplicationFactory<Program> factory)
    {
        _baseFactory = factory;
    }

    private HttpClient ClientWith(string mode, Func<HttpRequestMessage, HttpResponseMessage> downstream)
    {
        return _baseFactory.WithWebHostBuilder(builder =>
        {
            builder.UseTestAuth();
            builder.UseSetting("External:SystemMode", mode);
            builder.UseSetting("Erp:Url", "http://erp.test/erp");
            builder.UseSetting("Tax:Url", "http://tax.test/tax");
            builder.UseSetting("Clock:Url", "http://clock.test/clock");
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(
                    d => d.ServiceType == typeof(DbContextOptions<AppDbContext>));
                if (descriptor != null)
                    services.Remove(descriptor);
                services.RemoveAll<IDbContextOptionsConfiguration<AppDbContext>>();
                services.AddDbContext<AppDbContext>(options => options.UseInMemoryDatabase("StatusTestDb"));

                services.AddSingleton<IHttpClientFactory>(new StubHttpClientFactory(request =>
                {
                    _calls.Add(request.RequestUri!.ToString());
                    return downstream(request);
                }));
            });
        }).CreateClient();
    }

    [Fact]
    public async Task BackendIsUpWhenTheDatabaseAnswers()
    {
        var response = await ClientWith("stub", _ => new HttpResponseMessage(HttpStatusCode.OK)).GetAsync("/api/status/backend");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("{\"status\":\"UP\"}", await response.Content.ReadAsStringAsync());
        Assert.Empty(_calls);
    }

    [Fact]
    public async Task ErpIsUpWhenItsHealthEndpointReturns200()
    {
        var response = await ClientWith("stub", _ => new HttpResponseMessage(HttpStatusCode.OK)).GetAsync("/api/status/erp");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("{\"status\":\"UP\"}", await response.Content.ReadAsStringAsync());
        Assert.Equal(["http://erp.test/erp/health"], _calls);
    }

    [Fact]
    public async Task ErpIsDownWhenItsHealthEndpointReturns503()
    {
        var response = await ClientWith("stub", _ => new HttpResponseMessage(HttpStatusCode.ServiceUnavailable)).GetAsync("/api/status/erp");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        Assert.Equal("{\"status\":\"DOWN\"}", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task ErpIsDownWhenTheConnectionFails()
    {
        var response = await ClientWith("stub", _ => throw new HttpRequestException("connection refused")).GetAsync("/api/status/erp");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        Assert.Equal("{\"status\":\"DOWN\"}", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task TaxIsUpWhenItsHealthEndpointReturns200()
    {
        var response = await ClientWith("stub", _ => new HttpResponseMessage(HttpStatusCode.OK)).GetAsync("/api/status/tax");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal(["http://tax.test/tax/health"], _calls);
    }

    [Fact]
    public async Task ClockIsUpInRealModeWithoutAnyCall()
    {
        var response = await ClientWith("real", _ => throw new HttpRequestException("must not be called")).GetAsync("/api/status/clock");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("{\"status\":\"UP\"}", await response.Content.ReadAsStringAsync());
        Assert.Empty(_calls);
    }

    [Fact]
    public async Task ClockInStubModeProbesItsHealthEndpoint()
    {
        var response = await ClientWith("stub", _ => new HttpResponseMessage(HttpStatusCode.ServiceUnavailable)).GetAsync("/api/status/clock");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        Assert.Equal(["http://clock.test/clock/health"], _calls);
    }

    [Fact]
    public async Task UnknownSystemIsNotFound()
    {
        var response = await ClientWith("stub", _ => new HttpResponseMessage(HttpStatusCode.OK)).GetAsync("/api/status/nope");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    private sealed class StubHttpClientFactory(Func<HttpRequestMessage, HttpResponseMessage> respond) : IHttpClientFactory
    {
        public HttpClient CreateClient(string name) => new(new StubHandler(respond));
    }

    private sealed class StubHandler(Func<HttpRequestMessage, HttpResponseMessage> respond) : HttpMessageHandler
    {
        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken) =>
            Task.FromResult(respond(request));
    }
}
