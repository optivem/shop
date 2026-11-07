using System.Net;
using Microsoft.Extensions.Configuration;
using MyCompany.MyShop.Monolith.Core.Exceptions;
using MyCompany.MyShop.Monolith.Core.Services;
using MyCompany.MyShop.Monolith.Core.Services.External;
using Xunit;

namespace MyCompany.MyShop.Monolith.Tests;

/// <summary>ProductService proxies the ERP catalog; the ERP is stubbed at the HTTP boundary of ErpGateway.</summary>
public class ProductServiceTests
{
    private const string ErpCatalog = """
        [
          {"id":"HP-15","title":"HP 15 Laptop","description":"Everyday laptop","price":699.99,"category":"Laptops","brand":"HP"},
          {"id":"DELL-XPS","title":"Dell XPS 13","description":"Premium ultrabook","price":1299.99,"category":"Laptops","brand":"Dell"}
        ]
        """;

    private const string ErpBook = """
        {"id":"BOOK-123","title":"Clean Code","description":"A handbook of agile software craftsmanship","price":12.00,"category":"Books","brand":"Prentice Hall"}
        """;

    private static ProductService Service(StubErpHandler erp)
    {
        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?> { ["Erp:Url"] = "http://erp.test/erp" })
            .Build();
        return new ProductService(new ErpGateway(new HttpClient(erp), configuration));
    }

    [Fact]
    public async Task BrowseProducts_ProxiesTheErpCatalog()
    {
        var service = Service(new StubErpHandler().Returns("/api/products", HttpStatusCode.OK, ErpCatalog));

        var response = await service.BrowseProductsAsync();

        Assert.Equal(2, response.Count);
        Assert.Equal("HP-15", response[0].Sku);
        Assert.Equal("HP 15 Laptop", response[0].Title);
        Assert.Equal("Everyday laptop", response[0].Description);
        Assert.Equal("Laptops", response[0].Category);
        Assert.Equal("HP", response[0].Brand);
        Assert.Equal(699.99m, response[0].Price);
        Assert.Equal("DELL-XPS", response[1].Sku);
        Assert.Equal(1299.99m, response[1].Price);
    }

    [Fact]
    public async Task BrowseProducts_IsEmptyWhenTheErpHasNoProducts()
    {
        var service = Service(new StubErpHandler().Returns("/api/products", HttpStatusCode.OK, "[]"));

        var response = await service.BrowseProductsAsync();

        Assert.Empty(response);
    }

    [Fact]
    public async Task BrowseProducts_ThrowsErpUnavailableWhenTheErpFails()
    {
        var service = Service(new StubErpHandler().Returns("/api/products", HttpStatusCode.InternalServerError));

        await Assert.ThrowsAsync<ErpUnavailableException>(() => service.BrowseProductsAsync());
    }

    [Fact]
    public async Task ViewProduct_ProxiesTheErpProductDetails()
    {
        var service = Service(new StubErpHandler().Returns("/api/products/BOOK-123", HttpStatusCode.OK, ErpBook));

        var response = await service.ViewProductAsync("BOOK-123");

        Assert.Equal("BOOK-123", response.Sku);
        Assert.Equal("Clean Code", response.Title);
        Assert.Equal("A handbook of agile software craftsmanship", response.Description);
        Assert.Equal("Books", response.Category);
        Assert.Equal("Prentice Hall", response.Brand);
        Assert.Equal(12.00m, response.Price);
    }

    [Fact]
    public async Task ViewProduct_ThrowsNotExistWhenTheErpDoesNotKnowTheSku()
    {
        var service = Service(new StubErpHandler());

        var ex = await Assert.ThrowsAsync<NotExistValidationException>(() => service.ViewProductAsync("NO-SUCH-SKU"));

        Assert.Equal("Product NO-SUCH-SKU does not exist.", ex.Message);
    }
}
