using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Pages.Storefront;

namespace MyCompany.MyShop.Monolith.Pages
{
    /// <summary>`/checkout/confirmation` (customers only, storefront-ux §4.5): the orders just placed, and the rejected lines.</summary>
    [Authorize(Roles = Roles.Customer)]
    public class CheckoutConfirmationModel : PageModel
    {
        private readonly CatalogCache _catalog;
        private IReadOnlyList<ViewProductDetailsResponse> _products = [];

        public CheckoutConfirmationModel(CatalogCache catalog) => _catalog = catalog;

        public CheckoutResult? Result { get; private set; }

        /// <summary>The shop URL of the photo of a product, or null when the catalog is unavailable or the product has none.</summary>
        public string? ImageUrlOf(string sku) => _products.FirstOrDefault(product => product.Sku == sku)?.ImageUrl;

        public async Task OnGetAsync()
        {
            Result = CheckoutResult.Load(HttpContext.Session);
            _products = await _catalog.BrowseAsync() ?? [];
        }
    }
}
