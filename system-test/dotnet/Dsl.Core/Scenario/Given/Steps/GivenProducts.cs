using Common;
using Dsl.Port.Given.Steps;

namespace Dsl.Core.Scenario.Given;

public class GivenProducts : BaseGiven, IGivenProducts
{
    private readonly List<(string Sku, string? UnitPrice)> _products = new();

    public GivenProducts(GivenStage givenClause)
        : base(givenClause)
    {
    }

    public GivenProducts WithProduct(string sku, string? unitPrice)
    {
        _products.Add((sku, unitPrice));
        return this;
    }

    IGivenProducts IGivenProducts.WithProduct(string sku, string? unitPrice) => WithProduct(sku, unitPrice);

    public GivenProducts WithProduct(string sku, decimal unitPrice)
    {
        return WithProduct(sku, Converter.FromDecimal(unitPrice));
    }

    IGivenProducts IGivenProducts.WithProduct(string sku, decimal unitPrice) => WithProduct(sku, unitPrice);

    internal override async Task Execute(UseCaseDsl app)
    {
        var returnsProducts = app.Erp().ReturnsProducts();
        foreach (var (sku, unitPrice) in _products)
        {
            returnsProducts.Product(sku, unitPrice);
        }

        (await returnsProducts.Execute()).ShouldSucceed();
    }
}
