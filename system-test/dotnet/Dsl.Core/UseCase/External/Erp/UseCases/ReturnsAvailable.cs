using Driver.Port.External.Erp;
using Dsl.Core.External.Erp.UseCases.Base;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Erp.UseCases;

/// <summary>Brings the ERP back after an outage: its calls answer with what is staged again.</summary>
public class ReturnsAvailable : BaseErpUseCase<VoidValue, VoidVerification>
{
    public ReturnsAvailable(IErpDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<ErpUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var result = await _driver.ReturnsAvailableAsync();

        return new ErpUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
