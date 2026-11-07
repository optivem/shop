using System.Net;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Monolith.Core.Services.External;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>
/// The public catalog over HTTP: both product routes answer without a token or session, the list is a
/// bare JSON array, an unknown SKU is a 404 problem detail and an unavailable ERP a 503 problem detail.
/// </summary>
public class ProductApiTests : IClassFixture<ShopWebApplicationFactory>
{
    private const string ErpBook = """
        {"id":"BOOK-123","title":"Clean Code","description":"A handbook","price":12,"category":"Books","brand":"Prentice Hall"}
        """;

    private readonly ShopWebApplicationFactory _factory;

    public ProductApiTests(ShopWebApplicationFactory factory) => _factory = factory;

    private HttpClient AnonymousClient(StubErpHandler erp)
    {
        var factory = _factory.WithWebHostBuilder(builder => builder.ConfigureServices(services =>
            services.AddScoped(sp => new ErpGateway(new HttpClient(erp), sp.GetRequiredService<IConfiguration>()))));
        return factory.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
    }

    [Fact]
    public async Task BrowseProducts_WithoutToken_ReturnsTheCatalogAsABareArray()
    {
        var client = AnonymousClient(new StubErpHandler().Returns("/api/products", HttpStatusCode.OK, $"[{ErpBook}]"));

        var response = await client.GetAsync("/api/products");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync()).RootElement;
        Assert.Equal(JsonValueKind.Array, json.ValueKind);
        var product = json[0];
        Assert.Equal("BOOK-123", product.GetProperty("sku").GetString());
        Assert.Equal("Clean Code", product.GetProperty("title").GetString());
        Assert.Equal("A handbook", product.GetProperty("description").GetString());
        Assert.Equal("Books", product.GetProperty("category").GetString());
        Assert.Equal("Prentice Hall", product.GetProperty("brand").GetString());
        Assert.Equal("12.00", product.GetProperty("price").GetString());
    }

    [Fact]
    public async Task ViewProduct_WithoutToken_ReturnsTheProduct()
    {
        var client = AnonymousClient(new StubErpHandler().Returns("/api/products/BOOK-123", HttpStatusCode.OK, ErpBook));

        var response = await client.GetAsync("/api/products/BOOK-123");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync()).RootElement;
        Assert.Equal("BOOK-123", json.GetProperty("sku").GetString());
        Assert.Equal("Clean Code", json.GetProperty("title").GetString());
    }

    [Fact]
    public async Task ViewProduct_UnknownSku_ReturnsNotFoundProblemDetail()
    {
        var client = AnonymousClient(new StubErpHandler());

        var response = await client.GetAsync("/api/products/NO-SUCH-SKU");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        var body = await response.Content.ReadAsStringAsync();
        Assert.Contains("\"status\":404", body);
        Assert.Contains("Product NO-SUCH-SKU does not exist.", body);
    }

    [Fact]
    public async Task BrowseProducts_ErpDown_ReturnsServiceUnavailableProblemDetail()
    {
        var client = AnonymousClient(new StubErpHandler().Returns("/api/products", HttpStatusCode.ServiceUnavailable));

        var response = await client.GetAsync("/api/products");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        Assert.Contains("\"status\":503", await response.Content.ReadAsStringAsync());
    }
}
