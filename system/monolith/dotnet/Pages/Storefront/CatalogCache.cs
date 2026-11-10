using MyCompany.MyShop.Monolith.Core.Dtos;
using MyCompany.MyShop.Monolith.Core.Services;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>
/// The catalog for one request: a page that loads it and the header's categories share one ERP call.
/// </summary>
public sealed class CatalogCache
{
    private readonly ProductService _productService;
    private readonly ILogger<CatalogCache> _logger;
    private IReadOnlyList<ViewProductDetailsResponse>? _products;
    private bool _unavailable;

    public CatalogCache(ProductService productService, ILogger<CatalogCache> logger)
    {
        _productService = productService;
        _logger = logger;
    }

    /// <summary>The catalog, or null when the ERP can't be reached.</summary>
    public async Task<IReadOnlyList<ViewProductDetailsResponse>?> BrowseAsync()
    {
        if (_products != null || _unavailable)
        {
            return _products;
        }

        try
        {
            _products = await _productService.BrowseProductsAsync();
        }
        catch (Exception e)
        {
            _logger.LogWarning("Catalog unavailable: {Message}", e.Message);
            _unavailable = true;
        }

        return _products;
    }

    /// <summary>The header shows the catalog's categories on every page; an unavailable ERP leaves them out.</summary>
    public async Task<IReadOnlyList<string>> CategoriesAsync() =>
        CatalogQuery.CategoriesOf(await BrowseAsync() ?? []);
}
