using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;
using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Pages.Storefront;

namespace MyCompany.MyShop.Monolith.Pages
{
    /// <summary>`/` (storefront-ux §4.1, UC-006): the catalog, public. The query string holds the filters.</summary>
    [AllowAnonymous]
    public class IndexModel : PageModel
    {
        public const string ProductsUnavailable = "Products are temporarily unavailable.";

        private readonly CatalogCache _catalog;

        public IndexModel(CatalogCache catalog) => _catalog = catalog;

        public CatalogQuery Query { get; private set; } = CatalogQuery.Parse(null, null, null, null);

        public IReadOnlyList<ViewProductDetailsResponse> Shown { get; private set; } = [];

        public IReadOnlyList<string> FilterCategories { get; private set; } = [];

        public IReadOnlyList<string> FilterBrands { get; private set; } = [];

        public bool Unavailable { get; private set; }

        public async Task<IActionResult> OnGetAsync(
            [FromQuery] string? q, [FromQuery] string? category, [FromQuery(Name = "brand")] string[]? brands,
            [FromQuery] string? sort)
        {
            Query = CatalogQuery.Parse(q, category, brands, sort);
            if (CatalogQuery.HasDefaultParams(Request.Query))
            {
                return Redirect(Query.Path());
            }

            var products = await _catalog.BrowseAsync();
            Unavailable = products == null;
            Shown = Query.Apply(products ?? []);
            FilterCategories = CatalogQuery.CategoriesOf(products ?? []);
            FilterBrands = CatalogQuery.BrandsOf(products ?? []);
            ViewData[StoreChrome.SearchQueryKey] = Query.Q;
            return Page();
        }
    }
}
