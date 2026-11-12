using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using MyCompany.MyShop.Backend.Data;
using MyCompany.MyShop.Backend.Tests.Support;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Component;

/// <summary>The generated OpenAPI document is served at springdoc's URL and describes the API's endpoints.</summary>
public class OpenApiDocumentComponentTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public OpenApiDocumentComponentTests(WebApplicationFactory<Program> factory) => _factory = factory.WithWebHostBuilder(builder =>
    {
        builder.UseTestAuth();
        builder.ConfigureServices(services =>
        {
            var descriptor = services.SingleOrDefault(d => d.ServiceType == typeof(DbContextOptions<AppDbContext>));
            if (descriptor != null)
                services.Remove(descriptor);
            services.AddDbContext<AppDbContext>(options => options.UseInMemoryDatabase("OpenApiTestDb"));
        });
    });

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
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", TestAuth.CreateToken(["CUSTOMER"]));

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
