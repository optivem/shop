using Common;
using Dsl.Core.External.Erp.UseCases;
using Dsl.Port.Given.Steps;

namespace Dsl.Core.Scenario.Given;

public class GivenProducts : BaseGiven, IGivenProducts
{
    private readonly List<ReturnsProducts.ProductEntry> _products = new();

    public GivenProducts(GivenStage givenClause)
        : base(givenClause)
    {
    }

    public GivenProducts WithProduct(string sku, string? unitPrice)
    {
        _products.Add(new ReturnsProducts.ProductEntry(sku, unitPrice));
        return this;
    }

    IGivenProducts IGivenProducts.WithProduct(string sku, string? unitPrice) => WithProduct(sku, unitPrice);

    public GivenProducts WithProduct(string sku, decimal unitPrice)
    {
        return WithProduct(sku, Converter.FromDecimal(unitPrice));
    }

    IGivenProducts IGivenProducts.WithProduct(string sku, decimal unitPrice) => WithProduct(sku, unitPrice);

    public GivenProducts WithProduct(string sku, string title, string brand, string category, string unitPrice, string description)
    {
        _products.Add(new ReturnsProducts.ProductEntry(sku, unitPrice, title, brand, category, description));
        return this;
    }

    IGivenProducts IGivenProducts.WithProduct(string sku, string title, string brand, string category, string unitPrice, string description)
        => WithProduct(sku, title, brand, category, unitPrice, description);

    internal override async Task Execute(UseCaseDsl app)
    {
        var returnsProducts = app.Erp().ReturnsProducts();
        foreach (var product in _products)
        {
            returnsProducts.Product(product);
        }

        (await returnsProducts.Execute()).ShouldSucceed();
    }
}
