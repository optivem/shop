using Driver.Port;
using Dsl.Core.UseCase.UseCases.Base;
using Dsl.Core.Shared;
using Driver.Port.Dtos;

namespace Dsl.Core.UseCase.UseCases;

public class ViewProfile : BaseMyShopUseCase<ViewProfileResponse, ViewProfileVerification>
{
    private string? _whenAskedToSignIn;

    public ViewProfile(IMyShopDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    /// <summary>A visitor asked to sign in signs in as the customer (UI only).</summary>
    public ViewProfile SignInWhenAsked()
    {
        _whenAskedToSignIn = ViewProfileRequest.SignIn;
        return this;
    }

    public override async Task<MyShopUseCaseResult<ViewProfileResponse, ViewProfileVerification>> Execute()
    {
        var result = await _driver.ViewProfileAsync(new ViewProfileRequest { WhenAskedToSignIn = _whenAskedToSignIn });

        return new MyShopUseCaseResult<ViewProfileResponse, ViewProfileVerification>(
            result,
            _context,
            (response, ctx) => new ViewProfileVerification(response, ctx));
    }
}
