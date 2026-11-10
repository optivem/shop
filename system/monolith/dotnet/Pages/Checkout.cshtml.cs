using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Api.Security;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Services;
using MyCompany.MyShop.Monolith.Pages.Storefront;

namespace MyCompany.MyShop.Monolith.Pages
{
    /// <summary>
    /// `/checkout` (customers only, storefront-ux §4.4, UC-009): shipping address, optional coupon and the order
    /// review; places one order per cart line.
    /// </summary>
    [Authorize(Roles = Roles.Customer)]
    public class CheckoutModel : PageModel
    {
        public const string Saved = "saved";
        public const string Other = "other";
        private const string CartPath = "/cart";

        private readonly CatalogCache _catalog;
        private readonly ProfileService _profileService;
        private readonly CheckoutPlacer _placer;
        private readonly ILogger<CheckoutModel> _logger;

        public CheckoutModel(CatalogCache catalog, ProfileService profileService, CheckoutPlacer placer,
            ILogger<CheckoutModel> logger)
        {
            _catalog = catalog;
            _profileService = profileService;
            _placer = placer;
            _logger = logger;
        }

        public IReadOnlyList<PricedLine> Lines { get; private set; } = [];

        public decimal Subtotal { get; private set; }

        /// <summary>The customer's complete saved address, or null on a first order.</summary>
        public ShippingAddress? SavedAddress { get; private set; }

        public string Choice { get; private set; } = Other;

        /// <summary>The "Ship to a different address" form's values.</summary>
        public ShippingAddress Address { get; private set; } = ShippingAddresses.Empty();

        public IReadOnlyDictionary<string, string> FieldErrors { get; private set; } = new Dictionary<string, string>();

        public bool SaveAsDefault { get; private set; } = true;

        public string CouponCode { get; private set; } = string.Empty;

        public bool FirstOrder => SavedAddress == null;

        public string TaxCountry
        {
            get
            {
                var country = Choice == Saved && SavedAddress != null ? SavedAddress.Country : Address.Country;
                return string.IsNullOrWhiteSpace(country) ? "-" : country.Trim();
            }
        }

        public string TaxRate => ShippingAddresses.EstimatedTaxRate(TaxCountry);

        public async Task<IActionResult> OnGetAsync()
        {
            var cart = Cart.Load(HttpContext.Session);
            // An empty cart has nothing to check out (UC-009 3a).
            if (cart.IsEmpty)
            {
                return Redirect(CartPath);
            }

            var profile = await LoadProfileAsync(CurrentUserResolver.Resolve(User));
            SavedAddress = ShippingAddresses.SavedAddressOf(profile);
            Choice = SavedAddress != null ? Saved : Other;
            // "Ship to a different address" starts from the customer's own names, with the address left to fill in.
            var address = ShippingAddresses.Empty();
            if (profile != null)
            {
                address.FirstName = (SavedAddress ?? ShippingAddresses.AddressOf(profile)).FirstName;
                address.LastName = (SavedAddress ?? ShippingAddresses.AddressOf(profile)).LastName;
            }

            Address = address;
            await LoadLinesAsync(cart);
            return Page();
        }

        public async Task<IActionResult> OnPostAsync(
            [FromForm] string? addressChoice, [FromForm] string? firstName, [FromForm] string? lastName,
            [FromForm] string? street, [FromForm] string? postalCode, [FromForm] string? city, [FromForm] string? country,
            [FromForm] string? saveAsDefault, [FromForm] string? couponCode)
        {
            var cart = Cart.Load(HttpContext.Session);
            if (cart.IsEmpty)
            {
                return Redirect(CartPath);
            }

            var user = CurrentUserResolver.Resolve(User);
            SavedAddress = ShippingAddresses.SavedAddressOf(await LoadProfileAsync(user));
            Choice = SavedAddress != null && addressChoice == Saved ? Saved : Other;
            Address = new ShippingAddress
            {
                FirstName = firstName ?? string.Empty,
                LastName = lastName ?? string.Empty,
                Street = street ?? string.Empty,
                PostalCode = postalCode ?? string.Empty,
                City = city ?? string.Empty,
                Country = country ?? string.Empty,
            };
            SaveAsDefault = saveAsDefault != null;
            CouponCode = couponCode ?? string.Empty;

            IReadOnlyList<KeyValuePair<string, string>> errors = Choice == Saved ? [] : ShippingAddresses.Validate(Address);
            if (errors.Count > 0)
            {
                FieldErrors = errors.ToDictionary(error => error.Key, error => error.Value);
                Notice.Show(HttpContext, Notice.Error("Please correct the shipping address.", errors.Select(error => error.Value)));
                await LoadLinesAsync(cart);
                return Page();
            }

            var target = Choice == Saved ? SavedAddress! : Address.Trimmed();
            var code = string.IsNullOrWhiteSpace(CouponCode) ? null : CouponCode.Trim();
            var result = await _placer.PlaceAsync(cart.Lines, target, code, user);

            if (result.Placed.Count == 0)
            {
                // UC-009 8b: nothing was placed; the cart stays as it is.
                Notice.Show(HttpContext, Notice.Error("No order was placed.",
                    result.Rejected.Select(line => line.Sku + ": " + line.Reason)));
                await LoadLinesAsync(cart);
                return Page();
            }

            if (SavedAddress == null && SaveAsDefault)
            {
                await SaveAsDefaultAddressAsync(target, user);
            }

            // UC-009 8a: the placed lines leave the cart and the rejected ones stay in it.
            cart.RemoveAll(result.PlacedSkus);
            cart.Save(HttpContext.Session);
            result.Save(HttpContext.Session);
            return Redirect("/checkout/confirmation");
        }

        public string? FieldError(string field) => FieldErrors.GetValueOrDefault(field);

        private async Task LoadLinesAsync(Cart cart)
        {
            Lines = PricedLine.OfAll(cart, await _catalog.BrowseAsync() ?? []);
            Subtotal = Lines.Sum(line => line.LineTotal);
        }

        /// <summary>A profile that can't be read is treated like an empty one: checkout never blocks on it.</summary>
        private async Task<ViewProfileResponse?> LoadProfileAsync(CurrentUser user)
        {
            try
            {
                return await _profileService.ViewProfileAsync(user);
            }
            catch (Exception e)
            {
                _logger.LogWarning("Profile unavailable at checkout: {Message}", e.Message);
                return null;
            }
        }

        /// <summary>Best effort: the orders are placed either way.</summary>
        private async Task SaveAsDefaultAddressAsync(ShippingAddress address, CurrentUser user)
        {
            try
            {
                await _profileService.UpdateProfileAsync(new UpdateProfileRequest
                {
                    FirstName = address.FirstName,
                    LastName = address.LastName,
                    Street = address.Street,
                    PostalCode = address.PostalCode,
                    City = address.City,
                    Country = address.Country,
                }, user);
            }
            catch (Exception e)
            {
                _logger.LogWarning("Could not save the checkout address to the profile: {Message}", e.Message);
            }
        }
    }
}
