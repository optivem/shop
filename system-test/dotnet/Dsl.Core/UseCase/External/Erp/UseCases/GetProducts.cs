using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Dsl.Core.External.Erp.UseCases.Base;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

public class GetProducts : BaseErpUseCase<GetProductsResponse, GetProductsVerification>
{
    public GetProducts(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<ErpUseCaseResult<GetProductsResponse, GetProductsVerification>> Execute()
    {
        var result = await _driver.GetProductsAsync();

        return new ErpUseCaseResult<GetProductsResponse, GetProductsVerification>(
            result,
            _context,
            (response, ctx) => new GetProductsVerification(response, ctx));
    }
}
