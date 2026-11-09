using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

public class ViewProfile : BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification>
{
    public ViewProfile(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<MyShopUseCaseResult<ViewProfileResponse, ViewProfileVerification>> Execute()
    {
        var result = await _driver.ViewProfileAsync(new ViewProfileRequest());

        return new MyShopUseCaseResult<ViewProfileResponse, ViewProfileVerification>(
            result,
            _context,
            (response, ctx) => new ViewProfileVerification(response, ctx));
    }
}
