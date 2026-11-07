using DotNet.Testcontainers.Builders;
using DotNet.Testcontainers.Configurations;
using DotNet.Testcontainers.Containers;
using Microsoft.Extensions.Configuration;
using MyCompany.MyShop.Backend.Core.Exceptions;
using MyCompany.MyShop.Backend.Core.Services.External;
using System.Net.Http.Json;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Integration;

public sealed class ErpWireMockFixture : IAsyncLifetime
{
    private IContainer _container = null!;

    public string BaseUrl { get; private set; } = null!;

    public async Task InitializeAsync()
    {
        _container = new ContainerBuilder()
            .WithImage("wiremock/wiremock:3.9.0")
            .WithPortBinding(8080, assignRandomHostPort: true)
            .WithWaitStrategy(Wait.ForUnixContainer().UntilPortIsAvailable(8080))
            .Build();

        await _container.StartAsync();
        BaseUrl = $"http://localhost:{_container.GetMappedPublicPort(8080)}";
    }

    public async Task DisposeAsync() => await _container.DisposeAsync();
}

public class ErpGatewayIntegrationTest : IClassFixture<ErpWireMockFixture>, IAsyncLifetime
{
    private readonly HttpClient _adminClient;
    private readonly string _baseUrl;
    private ErpGateway _erpGateway = null!;

    public ErpGatewayIntegrationTest(ErpWireMockFixture fixture)
    {
        _baseUrl = fixture.BaseUrl;
        _adminClient = new HttpClient { BaseAddress = new Uri(_baseUrl) };
    }

    public async Task InitializeAsync()
    {
        await _adminClient.DeleteAsync("/__admin/mappings");

        var config = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["Erp:Url"] = _baseUrl })
            .Build();
        _erpGateway = new ErpGateway(new HttpClient(), config);
    }

    public Task DisposeAsync() => Task.CompletedTask;

    private Task<HttpResponseMessage> StubGetJsonAsync(string path, int status, object body) =>
        _adminClient.PostAsJsonAsync("/__admin/mappings", new
        {
            request = new { method = "GET", url = path },
            response = new { status, jsonBody = body }
        });

    private Task<HttpResponseMessage> StubGetStatusAsync(string path, int status) =>
        _adminClient.PostAsJsonAsync("/__admin/mappings", new
        {
            request = new { method = "GET", url = path },
            response = new { status }
        });

    [Fact]
    public async Task GetProductDetails_ReturnsDetails_WhenFound()
    {
        await StubGetJsonAsync("/api/products/BOOK-123", 200, new { id = "BOOK-123", price = 10.00m });

        var result = await _erpGateway.GetProductDetailsAsync("BOOK-123");

        Assert.NotNull(result);
        Assert.Equal("BOOK-123", result.Id);
        Assert.Equal(10.00m, result.Price);
    }

    [Fact]
    public async Task GetProductDetails_ReturnsNull_WhenNotFound()
    {
        await StubGetStatusAsync("/api/products/UNKNOWN", 404);

        var result = await _erpGateway.GetProductDetailsAsync("UNKNOWN");

        Assert.Null(result);
    }

    [Fact]
    public async Task GetProductDetails_Throws_OnServerError()
    {
        await StubGetStatusAsync("/api/products/BAD-SKU", 500);

        await Assert.ThrowsAsync<ErpUnavailableException>(
            () => _erpGateway.GetProductDetailsAsync("BAD-SKU"));
    }

    [Fact]
    public async Task GetPromotionDetails_ReturnsPromotion()
    {
        await StubGetJsonAsync("/api/promotion", 200, new { promotionActive = true, discount = 0.15m });

        var result = await _erpGateway.GetPromotionDetailsAsync();

        Assert.True(result.PromotionActive);
        Assert.Equal(0.15m, result.Discount);
    }

    [Fact]
    public async Task GetPromotionDetails_Throws_OnServerError()
    {
        await StubGetStatusAsync("/api/promotion", 503);

        await Assert.ThrowsAsync<ErpUnavailableException>(
            () => _erpGateway.GetPromotionDetailsAsync());
    }
}
