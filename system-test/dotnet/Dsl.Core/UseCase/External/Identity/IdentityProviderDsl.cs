using Driver.Port.External.Identity;
using Dsl.Core.External.Identity.UseCases;
using Dsl.Core.Shared;

namespace Dsl.Core.External.Identity;

/// <summary>
/// Not disposable, unlike the other external DSLs: <see cref="UseCaseDsl"/> disposes the identity provider driver
/// itself, last, because the shops' sessions may still be signed in as a user it created.
/// </summary>
public class IdentityProviderDsl
{
    private readonly IIdentityProviderDriver _driver;
    private readonly UseCaseContext _context;

    public IdentityProviderDsl(IIdentityProviderDriver driver, UseCaseContext context)
    {
        _driver = driver;
        _context = context;
    }

    public GoToIdentityProvider GoToIdentityProvider() => new(_driver, _context);
}
