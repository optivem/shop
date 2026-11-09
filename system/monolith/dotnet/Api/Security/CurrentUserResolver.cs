using System.Security.Claims;
using MyCompany.MyShop.Monolith.Core.Dtos;

namespace MyCompany.MyShop.Monolith.Api.Security;

/// <summary>
/// Resolves the caller from the authenticated principal (the token's <c>sub</c>, <c>preferred_username</c>,
/// <c>email</c>, <c>given_name</c> and <c>family_name</c>; inbound claim mapping is off, so the names are as issued).
/// </summary>
public static class CurrentUserResolver
{
    public static CurrentUser Resolve(ClaimsPrincipal principal) =>
        new(
            principal.FindFirst("sub")?.Value
                ?? throw new InvalidOperationException("Authenticated principal has no sub claim."),
            principal.FindFirst("preferred_username")?.Value,
            principal.IsInRole(Roles.Admin),
            principal.FindFirst("email")?.Value,
            principal.FindFirst("given_name")?.Value,
            principal.FindFirst("family_name")?.Value);
}
