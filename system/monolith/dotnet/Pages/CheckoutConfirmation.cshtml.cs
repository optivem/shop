using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Pages.Storefront;

namespace MyCompany.MyShop.Monolith.Pages
{
    /// <summary>`/checkout/confirmation` (customers only, storefront-ux §4.5): the orders just placed, and the rejected lines.</summary>
    [Authorize(Roles = Roles.Customer)]
    public class CheckoutConfirmationModel : PageModel
    {
        public CheckoutResult? Result { get; private set; }

        public void OnGet() => Result = CheckoutResult.Load(HttpContext.Session);
    }
}
