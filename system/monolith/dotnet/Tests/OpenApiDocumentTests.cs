using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>The generated OpenAPI document is served at springdoc's URL and describes the API's endpoints.</summary>
public class OpenApiDocumentTests : IClassFixture<ShopWebApplicationFactory>
{
    private readonly ShopWebApplicationFactory _factory;

    public OpenApiDocumentTests(ShopWebApplicationFactory factory) => _factory = factory;

    [Fact]
    public async Task OpenApiDocumentNeedsAnAuthenticatedCaller()
    {
        var client = _factory.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });

        var response = await client.GetAsync("/v3/api-docs");

        Assert.NotEqual(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task OpenApiDocumentDescribesTheApi()
    {
        var client = _factory.CreateClient(new WebApplicationFactoryClientOptions { AllowAutoRedirect = false });
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", _factory.CreateToken("CUSTOMER"));

        var response = await client.GetAsync("/v3/api-docs");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        using var document = JsonDocument.Parse(await response.Content.ReadAsStringAsync());
        Assert.Equal("MyShop API", document.RootElement.GetProperty("info").GetProperty("title").GetString());
        var paths = document.RootElement.GetProperty("paths");
        foreach (var path in new[] { "/api/products", "/api/products/{sku}", "/api/orders", "/api/orders/{orderNumber}", "/api/coupons" })
        {
            Assert.True(paths.TryGetProperty(path, out _), $"Expected the OpenAPI document to describe {path}");
        }
    }
}
