using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Pages.Storefront;

namespace MyCompany.MyShop.Monolith.Pages
{
    /// <summary>
    /// `/cart` (storefront-ux §4.3, UC-008): the session cart at current catalog prices. Changes are plain form posts
    /// to `/cart/add`, `/cart/update` and `/cart/remove` that redirect back (PRG), so the cart works without
    /// JavaScript (storefront-ux §6.3).
    /// </summary>
    [AllowAnonymous]
    public class CartModel : PageModel
    {
        public const string AddedToCart = "Added to cart";
        private const string CartPath = "/cart";

        private readonly CatalogCache _catalog;

        public CartModel(CatalogCache catalog) => _catalog = catalog;

        public IReadOnlyList<PricedLine> Lines { get; private set; } = [];

        public decimal Subtotal { get; private set; }

        public bool Unavailable { get; private set; }

        public async Task OnGetAsync()
        {
            var cart = Cart.Load(HttpContext.Session);
            if (cart.IsEmpty)
            {
                return;
            }

            // Prices are re-read from the catalog whenever the cart is shown (UC-008 BR-5).
            var products = await _catalog.BrowseAsync();
            Unavailable = products == null;
            Lines = PricedLine.OfAll(cart, products ?? []);
            Subtotal = Lines.Sum(line => line.LineTotal);
        }

        /// <summary>"Add to cart" from the catalog (quantity 1) or the product page; goes back to where it was clicked.</summary>
        public IActionResult OnPostAdd([FromForm] string? sku, [FromForm] string? quantity, [FromForm] string? returnUrl)
        {
            var parsed = Quantity.Parse(quantity ?? "1");
            if (string.IsNullOrWhiteSpace(sku))
            {
                Notice.Flash(HttpContext.Session, Notice.Error("SKU must not be empty"));
            }
            else if (parsed == null)
            {
                Notice.Flash(HttpContext.Session, Notice.Error(Quantity.Error));
            }
            else
            {
                var cart = Cart.Load(HttpContext.Session);
                cart.Add(sku, parsed.Value);
                cart.Save(HttpContext.Session);
                Notice.Flash(HttpContext.Session, Notice.Success(AddedToCart));
            }

            return Redirect(ReturnUrls.Safe(returnUrl));
        }

        /// <summary>A typed quantity is checked when committed; one that is not 1..99 keeps the previous quantity.</summary>
        public IActionResult OnPostUpdate([FromForm] string? sku, [FromForm] string? quantity)
        {
            var parsed = Quantity.Parse(quantity);
            if (parsed == null)
            {
                Notice.Flash(HttpContext.Session, Notice.Error(Quantity.Error));
            }
            else if (sku != null)
            {
                var cart = Cart.Load(HttpContext.Session);
                cart.SetQuantity(sku, parsed.Value);
                cart.Save(HttpContext.Session);
            }

            return Redirect(CartPath);
        }

        public IActionResult OnPostRemove([FromForm] string? sku)
        {
            if (sku != null)
            {
                var cart = Cart.Load(HttpContext.Session);
                cart.Remove(sku);
                cart.Save(HttpContext.Session);
            }

            return Redirect(CartPath);
        }
    }
}
