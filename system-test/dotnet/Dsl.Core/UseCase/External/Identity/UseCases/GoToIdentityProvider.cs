using Dsl.Core.External.Identity.UseCases.Base;
using Driver.Port.External.Identity;
using Common;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Identity.UseCases;

public class GoToIdentityProvider : BaseIdentityProviderUseCase<VoidValue, VoidVerification>
{
    public GoToIdentityProvider(IIdentityProviderDriver driver, UseCaseContext context)
        : base(driver, context)
    {
    }

    public override async Task<IdentityProviderUseCaseResult<VoidValue, VoidVerification>> Execute()
    {
        var result = await _driver.GoToIdentityProviderAsync();

        return new IdentityProviderUseCaseResult<VoidValue, VoidVerification>(
            result,
            _context,
            (response, ctx) => new VoidVerification(response, ctx));
    }
}
