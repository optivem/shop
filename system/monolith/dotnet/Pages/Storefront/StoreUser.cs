using System.Security.Claims;
using MyCompany.MyShop.Monolith.Api.Security;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>Who the header shows as signed in, and which links they get (storefront-ux §3).</summary>
public sealed record StoreUser(string Username, bool Admin, bool CanPlaceOrder)
{
    /// <summary>The signed-in user, or null for an anonymous visitor.</summary>
    public static StoreUser? Of(ClaimsPrincipal principal)
    {
        if (principal.Identity?.IsAuthenticated != true)
        {
            return null;
        }

        var username = principal.FindFirst("preferred_username")?.Value ?? principal.Identity.Name ?? string.Empty;
        return new StoreUser(username, principal.IsInRole(Roles.Admin), principal.IsInRole(Roles.Customer));
    }
}
