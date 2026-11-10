using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Dsl.Core.External.Erp.UseCases.Base;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

/// <summary>
/// Stages the ERP's enumerable product list (GET /api/products), in the order the products are added; an empty
/// list stages no products. Unset details fall back to the driver's defaults.
/// </summary>
public class ReturnsProducts : BaseErpUseCase<VoidValue, VoidVerification>
{
    /// <summary>One staged product; the SKU is an alias.</summary>
    public record ProductEntry(
        string SkuParamAlias,
        string? UnitPrice,
        string? Title = null,
        string? Brand = null,
        string? Category = null,
        string? Description = null);

    private readonly List<ProductEntry> _products = new();

    public ReturnsProducts(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public ReturnsProducts Product(string skuParamAlias, string? unitPrice)
    {
        return Product(new ProductEntry(skuParamAlias, unitPrice));
    }

    public ReturnsProducts Product(string skuParamAlias, decimal unitPrice)
    {
        return Product(skuParamAlias, unitPrice.ToString());
    }

    public ReturnsProducts Product(ProductEntry product)
    {
        _products.Add(product);
        return this;
    }

    public override async Task<ErpUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var request = new ReturnsProductsRequest
        {
            Products = _products
                .Select(product => new ReturnsProductRequest
                {
                    Sku = _context.GetParamValue(product.SkuParamAlias),
                    Price = product.UnitPrice,
                    Title = product.Title,
                    Brand = product.Brand,
                    Category = product.Category,
                    Description = product.Description,
                })
                .ToList()
        };

        var result = await _driver.ReturnsProductsAsync(request);

        return new ErpUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
