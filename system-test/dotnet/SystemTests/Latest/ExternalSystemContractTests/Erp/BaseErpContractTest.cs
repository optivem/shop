using SystemTests.Latest.ExternalSystemContractTests.Base;

namespace SystemTests.Latest.ExternalSystemContractTests.Erp;

public abstract class BaseErpContractTest : BaseExternalSystemContractTest
{
    [Fact]
    public async Task ShouldBeAbleToGetProduct()
    {
        await Scenario()
            .Given().Product().WithSku("BOOK-123").WithUnitPrice(12.0m)
            .Then().Product("BOOK-123")
            .HasSku("BOOK-123")
            .HasPrice(12.0m);
    }

    [Fact]
    public async Task ShouldBeAbleToGetProductDetails()
    {
        await Scenario()
            .Given().Product()
                .WithSku("BOOK-123")
                .WithUnitPrice(12.0m)
                .WithTitle("Clean Code")
                .WithDescription("A handbook of agile software craftsmanship")
                .WithCategory("Books")
                .WithBrand("Prentice Hall")
            .Then().Product("BOOK-123")
                .HasTitle("Clean Code")
                .HasDescription("A handbook of agile software craftsmanship")
                .HasCategory("Books")
                .HasBrand("Prentice Hall");
    }

    [Fact]
    public async Task ShouldBeAbleToListProducts()
    {
        await Scenario()
            .Given().Products()
                .WithProduct("BOOK-123", 12.00m)
                .WithProduct("BOOK-456", 15.50m)
            .Then().Products()
                .ContainsProduct("BOOK-123", 12.00m)
                .ContainsProduct("BOOK-456", 15.50m);
    }
}










