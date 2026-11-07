using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Dsl.Core.External.Erp.UseCases.Base;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

/// <summary>Stages the ERP's enumerable product list (GET /api/products); an empty list stages no products.</summary>
public class ReturnsProducts : BaseErpUseCase<VoidValue, VoidVerification>
{
    private readonly List<(string SkuParamAlias, string? UnitPrice)> _products = new();

    public ReturnsProducts(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public ReturnsProducts Product(string skuParamAlias, string? unitPrice)
    {
        _products.Add((skuParamAlias, unitPrice));
        return this;
    }

    public ReturnsProducts Product(string skuParamAlias, decimal unitPrice)
    {
        return Product(skuParamAlias, unitPrice.ToString());
    }

    public override async Task<ErpUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var request = new ReturnsProductsRequest
        {
            Products = _products
                .Select(product => new ReturnsProductRequest
                {
                    Sku = _context.GetParamValue(product.SkuParamAlias),
                    Price = product.UnitPrice
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
