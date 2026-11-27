using Driver.Port.External.Identity;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Identity.UseCases.Base;

public abstract class BaseIdentityProviderUseCase<TResponse, TVerification>
    where TVerification : ResponseVerification<TResponse>
{
    protected readonly IIdentityProviderDriver _driver;
    protected readonly UseCaseContext _context;

    protected BaseIdentityProviderUseCase(IIdentityProviderDriver driver, UseCaseContext context)
    {
        _driver = driver;
        _context = context;
    }

    public abstract Task<IdentityProviderUseCaseResult<TResponse, TVerification>> Execute();
}
