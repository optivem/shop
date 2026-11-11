using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Dsl.Core.External.Erp.UseCases.Base;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

/// <summary>The ERP no longer offers a product: asking for it by SKU (an alias) answers "not found".</summary>
public class ReturnsProductNotFound : BaseErpUseCase<VoidValue, VoidVerification>
{
    private string? _skuParamAlias;

    public ReturnsProductNotFound(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public ReturnsProductNotFound Sku(string? skuParamAlias)
    {
        _skuParamAlias = skuParamAlias;
        return this;
    }

    public override async Task<ErpUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var request = new GetProductRequest
        {
            Sku = _context.GetParamValue(_skuParamAlias)
        };

        var result = await _driver.ReturnsProductNotFoundAsync(request);

        return new ErpUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
