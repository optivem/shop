using System.Security.Claims;
using MyCompany.MyShop.Backend.Core.Dtos;

namespace MyCompany.MyShop.Backend.Api.Security;

/// <summary>Resolves the caller from the authenticated principal (the token's <c>sub</c> and <c>preferred_username</c>).</summary>
public static class CurrentUserResolver
{
    public static CurrentUser Resolve(ClaimsPrincipal principal) =>
        new(
            principal.FindFirst("sub")?.Value
                ?? throw new InvalidOperationException("Authenticated principal has no sub claim."),
            principal.FindFirst("preferred_username")?.Value,
            principal.IsInRole(Roles.Admin));
}
