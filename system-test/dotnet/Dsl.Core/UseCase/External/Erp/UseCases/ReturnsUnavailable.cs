using Driver.Port.External.Erp;
using Dsl.Core.External.Erp.UseCases.Base;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

/// <summary>Takes the ERP down: every ERP API call fails, so the system cannot retrieve product information.</summary>
public class ReturnsUnavailable : BaseErpUseCase<VoidValue, VoidVerification>
{
    public ReturnsUnavailable(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<ErpUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var result = await _driver.ReturnsUnavailableAsync();

        return new ErpUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
