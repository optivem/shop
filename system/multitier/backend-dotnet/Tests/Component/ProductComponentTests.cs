using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.EntityFrameworkCore.Infrastructure;
using System.Net;
using System.Text.Json;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Moq;
using MyCompany.MyShop.Backend.Core.Dtos.External;
using MyCompany.MyShop.Backend.Core.Exceptions;
using MyCompany.MyShop.Backend.Core.Services.External;
using MyCompany.MyShop.Backend.Data;
using MyCompany.MyShop.Backend.Tests.Support;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Component;

/// <summary>
/// The public catalog over HTTP: every product route answers without a token, the list is a bare JSON
/// array, an unknown SKU is a 404 problem detail and an unavailable ERP a 503 problem detail.
/// </summary>
public class ProductComponentTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly Mock<ErpGateway> _erpMock = new(new HttpClient(), new Mock<IConfiguration>().Object);
    private readonly WebApplicationFactory<Program> _factory;

    public ProductComponentTests(WebApplicationFactory<Program> factory)
    {
        _factory = factory.WithWebHostBuilder(builder =>
        {
            builder.UseTestAuth();
            builder.ConfigureServices(services =>
            {
                var descriptor = services.SingleOrDefault(
                    d => d.ServiceType == typeof(DbContextOptions<AppDbContext>));
                if (descriptor != null)
                    services.Remove(descriptor);
                services.RemoveAll<IDbContextOptionsConfiguration<AppDbContext>>();

                services.AddDbContext<AppDbContext>(options =>
                    options.UseInMemoryDatabase("ProductTestDb"));

                services.AddScoped(_ => _erpMock.Object);
            });
        });
    }

    [Fact]
    public async Task BrowseProductsWithoutTokenReturnsTheCatalogAsABareArray()
    {
        _erpMock.Setup(g => g.GetProductsAsync()).ReturnsAsync(new List<ErpProductDetailsResponse>
        {
            new() { Id = "BOOK-123", Title = "Clean Code", Description = "A handbook", Category = "Books", Brand = "Prentice Hall", Price = 12m }
        });

        var response = await _factory.CreateClient().GetAsync("/api/products");

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
    public async Task ViewProductWithoutTokenReturnsTheProduct()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("BOOK-123"))
            .ReturnsAsync(new ErpProductDetailsResponse { Id = "BOOK-123", Title = "Clean Code", Price = 12m });

        var response = await _factory.CreateClient().GetAsync("/api/products/BOOK-123");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var json = JsonDocument.Parse(await response.Content.ReadAsStringAsync()).RootElement;
        Assert.Equal("BOOK-123", json.GetProperty("sku").GetString());
        Assert.Equal("Clean Code", json.GetProperty("title").GetString());
        Assert.Equal(JsonValueKind.Null, json.GetProperty("imageUrl").ValueKind);
    }

    [Fact]
    public async Task ViewUnknownProductReturnsNotFoundProblemDetail()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("NO-SUCH-SKU")).ReturnsAsync((ErpProductDetailsResponse?)null);

        var response = await _factory.CreateClient().GetAsync("/api/products/NO-SUCH-SKU");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Equal("application/problem+json", response.Content.Headers.ContentType?.MediaType);
        Assert.Contains("Product NO-SUCH-SKU does not exist.", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task ViewProductImageWithoutTokenReturnsTheErpImage()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("BOOK-123"))
            .ReturnsAsync(new ErpProductDetailsResponse { Id = "BOOK-123", Price = 12m, ImageUrl = "/erp/images/BOOK-123.jpg" });
        _erpMock.Setup(g => g.GetProductImageAsync("/erp/images/BOOK-123.jpg"))
            .ReturnsAsync(new ErpProductImage(new byte[] { 1, 2, 3 }, "image/jpeg"));

        var response = await _factory.CreateClient().GetAsync("/api/products/BOOK-123/image");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        Assert.Equal("image/jpeg", response.Content.Headers.ContentType?.MediaType);
        Assert.Equal(new byte[] { 1, 2, 3 }, await response.Content.ReadAsByteArrayAsync());
    }

    [Fact]
    public async Task ViewImageOfUnknownProductReturnsNotFoundProblemDetail()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("NO-SUCH-SKU")).ReturnsAsync((ErpProductDetailsResponse?)null);

        var response = await _factory.CreateClient().GetAsync("/api/products/NO-SUCH-SKU/image");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        Assert.Equal("application/problem+json", response.Content.Headers.ContentType?.MediaType);
    }

    [Fact]
    public async Task BrowseProductsWhenErpIsDownReturnsServiceUnavailableProblemDetail()
    {
        _erpMock.Setup(g => g.GetProductsAsync()).ThrowsAsync(new ErpUnavailableException("ERP down"));

        var response = await _factory.CreateClient().GetAsync("/api/products");

        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
        Assert.Equal("application/problem+json", response.Content.Headers.ContentType?.MediaType);
    }
}
