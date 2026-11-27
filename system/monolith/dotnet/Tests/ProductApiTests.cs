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

    private const string ErpHp15 = """
        {"id":"HP-15","title":"HP Laptop 15","price":699.99,"category":"Laptops","brand":"HP","imageUrl":"/erp/images/HP-15.jpg"}
        """;

    [Fact]
    public async Task ViewProduct_ExposesTheShopImageUrl()
    {
        var client = AnonymousClient(new StubErpHandler().Returns("/api/products/HP-15", HttpStatusCode.OK, ErpHp15));

        var json = JsonDocument.Parse(await client.GetStringAsync("/api/products/HP-15")).RootElement;

        Assert.Equal("/api/products/HP-15/image", json.GetProperty("imageUrl").GetString());
    }

    [Fact]
    public async Task ViewProduct_WithoutAnErpImage_HasANullImageUrl()
    {
        var client = AnonymousClient(new StubErpHandler().Returns("/api/products/BOOK-123", HttpStatusCode.OK, ErpBook));

        var json = JsonDocument.Parse(await client.GetStringAsync("/api/products/BOOK-123")).RootElement;

        Assert.Equal(JsonValueKind.Null, json.GetProperty("imageUrl").ValueKind);
    }

    [Fact]
    public async Task ViewProductImage_WithoutToken_ReturnsTheErpImage()
    {
        var client = AnonymousClient(new StubErpHandler()
            .Returns("/api/products/HP-15", HttpStatusCode.OK, ErpHp15)
            .ReturnsImage("/erp/images/HP-15.jpg", [1, 2, 3]));

        var response = await client.GetAsync("/api/products/HP-15/image");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("image/jpeg", response.Content.Headers.ContentType?.MediaType);
        Assert.Equal(new byte[] { 1, 2, 3 }, await response.Content.ReadAsByteArrayAsync());
    }

    [Fact]
    public async Task ViewProductImage_UnknownSku_ReturnsNotFoundProblemDetail()
    {
        var client = AnonymousClient(new StubErpHandler());

        var response = await client.GetAsync("/api/products/NO-SUCH-SKU/image");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Contains("Product NO-SUCH-SKU does not exist.", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task ViewProductImage_ProductWithoutImage_ReturnsNotFoundProblemDetail()
    {
        var client = AnonymousClient(new StubErpHandler().Returns("/api/products/BOOK-123", HttpStatusCode.OK, ErpBook));

        var response = await client.GetAsync("/api/products/BOOK-123/image");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Contains("Product BOOK-123 has no image.", await response.Content.ReadAsStringAsync());
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
