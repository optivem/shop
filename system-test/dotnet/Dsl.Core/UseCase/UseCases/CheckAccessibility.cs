using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

/// <summary>Runs axe on the page the last step left open (FT Step 4). UI channel only.</summary>
public class CheckAccessibility : BaseMyShopUseCase<CheckAccessibilityResponse, CheckAccessibilityVerification>
{
    public CheckAccessibility(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<MyShopUseCaseResult<CheckAccessibilityResponse, CheckAccessibilityVerification>> Execute()
    {
        var result = await _driver.CheckAccessibilityAsync(new CheckAccessibilityRequest());

        return new MyShopUseCaseResult<CheckAccessibilityResponse, CheckAccessibilityVerification>(
            result,
            _context,
            (response, ctx) => new CheckAccessibilityVerification(response, ctx));
    }
}
