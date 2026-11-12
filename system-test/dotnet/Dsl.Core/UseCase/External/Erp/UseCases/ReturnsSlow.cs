using Driver.Port.External.Erp;
using Dsl.Core.External.Erp.UseCases.Base;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

/// <summary>Makes the ERP slow: every ERP API call answers only after the system's outbound timeout has passed.</summary>
public class ReturnsSlow : BaseErpUseCase<VoidValue, VoidVerification>
{
    public ReturnsSlow(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<ErpUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var result = await _driver.ReturnsSlowAsync();

        return new ErpUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
