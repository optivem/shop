using Microsoft.Extensions.Configuration;
using Moq;
using MyCompany.MyShop.Backend.Core.Dtos.External;
using MyCompany.MyShop.Backend.Core.Exceptions;
using MyCompany.MyShop.Backend.Core.Services;
using MyCompany.MyShop.Backend.Core.Services.External;
using Xunit;

namespace MyCompany.MyShop.Backend.Tests.Unit;

public class ProductServiceTest
{
    private readonly Mock<ErpGateway> _erpMock;
    private readonly ProductService _service;

    public ProductServiceTest()
    {
        _erpMock = new Mock<ErpGateway>(new HttpClient(), new Mock<IConfiguration>().Object);
        _service = new ProductService(_erpMock.Object);
    }

    [Fact]
    public async Task BrowseProducts_ProxiesTheErpCatalog()
    {
        _erpMock.Setup(g => g.GetProductsAsync()).ReturnsAsync(new List<ErpProductDetailsResponse>
        {
            Product("HP-15", 699.99m, "HP 15 Laptop", "Everyday laptop", "Laptops", "HP"),
            Product("DELL-XPS", 1299.99m, "Dell XPS 13", "Premium ultrabook", "Laptops", "Dell")
        });

        var response = await _service.BrowseProductsAsync();

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
        _erpMock.Setup(g => g.GetProductsAsync()).ReturnsAsync(new List<ErpProductDetailsResponse>());

        var response = await _service.BrowseProductsAsync();

        Assert.Empty(response);
    }

    [Fact]
    public async Task ViewProduct_ProxiesTheErpProductDetails()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("BOOK-123"))
            .ReturnsAsync(Product("BOOK-123", 12.00m, "Clean Code", "A handbook of agile software craftsmanship", "Books", "Prentice Hall"));

        var response = await _service.ViewProductAsync("BOOK-123");

        Assert.Equal("BOOK-123", response.Sku);
        Assert.Equal("Clean Code", response.Title);
        Assert.Equal("A handbook of agile software craftsmanship", response.Description);
        Assert.Equal("Books", response.Category);
        Assert.Equal("Prentice Hall", response.Brand);
        Assert.Equal(12.00m, response.Price);
        Assert.Equal("/api/products/BOOK-123/image", response.ImageUrl);
    }

    [Fact]
    public async Task ViewProduct_HasNoImageUrlWhenTheErpHasNoImage()
    {
        var product = Product("BOOK-123", 12.00m, "Clean Code", "A handbook", "Books", "Prentice Hall");
        product.ImageUrl = null;
        _erpMock.Setup(g => g.GetProductDetailsAsync("BOOK-123")).ReturnsAsync(product);

        var response = await _service.ViewProductAsync("BOOK-123");

        Assert.Null(response.ImageUrl);
    }

    [Fact]
    public async Task GetProductImage_ProxiesTheErpImage()
    {
        var image = new ErpProductImage(new byte[] { 1, 2, 3 }, "image/jpeg");
        _erpMock.Setup(g => g.GetProductDetailsAsync("HP-15"))
            .ReturnsAsync(Product("HP-15", 699.99m, "HP 15 Laptop", "Everyday laptop", "Laptops", "HP"));
        _erpMock.Setup(g => g.GetProductImageAsync("/erp/images/HP-15.jpg")).ReturnsAsync(image);

        var result = await _service.GetProductImageAsync("HP-15");

        Assert.Same(image, result);
    }

    [Fact]
    public async Task GetProductImage_ThrowsNotExistWhenTheErpDoesNotKnowTheSku()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("NO-SUCH-SKU")).ReturnsAsync((ErpProductDetailsResponse?)null);

        var ex = await Assert.ThrowsAsync<NotExistValidationException>(() => _service.GetProductImageAsync("NO-SUCH-SKU"));

        Assert.Equal("Product NO-SUCH-SKU does not exist.", ex.Message);
        _erpMock.Verify(g => g.GetProductImageAsync(It.IsAny<string>()), Times.Never);
    }

    [Fact]
    public async Task GetProductImage_ThrowsNotExistWhenTheProductHasNoImage()
    {
        var product = Product("HP-15", 699.99m, "HP 15 Laptop", "Everyday laptop", "Laptops", "HP");
        product.ImageUrl = null;
        _erpMock.Setup(g => g.GetProductDetailsAsync("HP-15")).ReturnsAsync(product);

        var ex = await Assert.ThrowsAsync<NotExistValidationException>(() => _service.GetProductImageAsync("HP-15"));

        Assert.Equal("Product HP-15 has no image.", ex.Message);
        _erpMock.Verify(g => g.GetProductImageAsync(It.IsAny<string>()), Times.Never);
    }

    [Fact]
    public async Task GetProductImage_ThrowsNotExistWhenTheErpHasNoSuchImage()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("HP-15"))
            .ReturnsAsync(Product("HP-15", 699.99m, "HP 15 Laptop", "Everyday laptop", "Laptops", "HP"));
        _erpMock.Setup(g => g.GetProductImageAsync("/erp/images/HP-15.jpg")).ReturnsAsync((ErpProductImage?)null);

        var ex = await Assert.ThrowsAsync<NotExistValidationException>(() => _service.GetProductImageAsync("HP-15"));

        Assert.Equal("Image of product HP-15 does not exist.", ex.Message);
    }

    [Fact]
    public async Task ViewProduct_ThrowsNotExistWhenTheErpDoesNotKnowTheSku()
    {
        _erpMock.Setup(g => g.GetProductDetailsAsync("NO-SUCH-SKU")).ReturnsAsync((ErpProductDetailsResponse?)null);

        var ex = await Assert.ThrowsAsync<NotExistValidationException>(() => _service.ViewProductAsync("NO-SUCH-SKU"));

        Assert.Equal("Product NO-SUCH-SKU does not exist.", ex.Message);
    }

    private static ErpProductDetailsResponse Product(string sku, decimal price, string title, string description, string category, string brand)
        => new()
        {
            Id = sku,
            Price = price,
            Title = title,
            Description = description,
            Category = category,
            Brand = brand,
            ImageUrl = $"/erp/images/{sku}.jpg"
        };
}
