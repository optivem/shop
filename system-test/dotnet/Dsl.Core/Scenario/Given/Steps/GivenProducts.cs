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

    internal override Task Execute(UseCaseDsl app) => StageAsync(app, null, null);

    /// <summary>Stages the list again with this product at a new price; a list without the product is left as it is.</summary>
    internal async Task ExecuteWithPriceAsync(UseCaseDsl app, string skuAlias, string unitPrice)
    {
        if (_products.Any(product => product.SkuParamAlias == skuAlias))
        {
            await StageAsync(app, skuAlias, unitPrice);
        }
    }

    /// <summary>Stages the list again without this product; a list without the product is left as it is.</summary>
    internal async Task ExecuteWithoutAsync(UseCaseDsl app, string skuAlias)
    {
        if (_products.Any(product => product.SkuParamAlias == skuAlias))
        {
            await StageAsync(app, null, null, skuAlias);
        }
    }

    private Task StageAsync(UseCaseDsl app, string? changedSkuAlias, string? changedUnitPrice)
        => StageAsync(app, changedSkuAlias, changedUnitPrice, null);

    private async Task StageAsync(UseCaseDsl app, string? changedSkuAlias, string? changedUnitPrice, string? withdrawnSkuAlias)
    {
        var returnsProducts = app.Erp().ReturnsProducts();
        foreach (var product in _products)
        {
            if (product.SkuParamAlias == withdrawnSkuAlias)
            {
                continue;
            }
            returnsProducts.Product(product.SkuParamAlias == changedSkuAlias
                ? product with { UnitPrice = changedUnitPrice }
                : product);
        }

        (await returnsProducts.Execute()).ShouldSucceed();
    }
}
