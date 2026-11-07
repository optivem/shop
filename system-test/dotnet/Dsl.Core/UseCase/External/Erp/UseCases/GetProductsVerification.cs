using Driver.Port.External.Erp.Dtos;
using Dsl.Core.Shared;
using Shouldly;

namespace Dsl.Core.External.Erp.UseCases;

/// <summary>
/// Asserts on the products a scenario staged, not on the list's size: the real ERP also lists
/// products seeded or created by other tests.
/// </summary>
public class GetProductsVerification : ResponseVerification<GetProductsResponse>
{
    public GetProductsVerification(GetProductsResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public GetProductsVerification ContainsProduct(string skuParamAlias, decimal expectedPrice)
    {
        var expectedSku = Context.GetParamValue(skuParamAlias);
        var product = FindProduct(expectedSku);
        var actualPrice = product.Price;
        actualPrice.ShouldBe(expectedPrice, $"Expected product '{expectedSku}' to have price {expectedPrice}, but was {actualPrice}");
        return this;
    }

    public GetProductsVerification ContainsProduct(string skuParamAlias, string expectedPrice)
    {
        return ContainsProduct(skuParamAlias, decimal.Parse(expectedPrice));
    }

    private GetProductResponse FindProduct(string? sku)
    {
        var products = Response.Products;
        var product = products.FirstOrDefault(p => p.Sku == sku);
        product.ShouldNotBeNull(
            $"Expected product list to contain SKU '{sku}', but it contained [{string.Join(", ", products.Select(p => p.Sku))}]");
        return product;
    }
}
