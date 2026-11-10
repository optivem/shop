namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>
/// What the layout's header, footer and account sidebar need on every page: the signed-in user, the cart badge
/// count, the catalog's categories, the notice to show and the current address (for "Sign in" to return to).
/// </summary>
public sealed class StoreChrome
{
    /// <summary>The ViewData key of the header search box's value (the catalog sets it to its search term).</summary>
    public const string SearchQueryKey = "SearchQuery";

    private readonly IHttpContextAccessor _httpContextAccessor;
    private readonly CatalogCache _catalog;

    public StoreChrome(IHttpContextAccessor httpContextAccessor, CatalogCache catalog)
    {
        _httpContextAccessor = httpContextAccessor;
        _catalog = catalog;
    }

    private HttpContext Context => _httpContextAccessor.HttpContext
        ?? throw new InvalidOperationException("The storefront chrome is only rendered within a request.");

    public StoreUser? User => StoreUser.Of(Context.User);

    public int CartCount => Cart.Load(Context.Session).TotalQuantity;

    public string CurrentPath => Context.Request.Path.Value ?? "/";

    /// <summary>The current page address, path and query.</summary>
    public string ReturnUrl => CurrentPath + Context.Request.QueryString.Value;

    public string SignInHref => "/sign-in?returnUrl=" + Uri.EscapeDataString(ReturnUrl);

    public Task<IReadOnlyList<string>> CategoriesAsync() => _catalog.CategoriesAsync();

    public Notice? TakeNotice() => Notice.Take(Context);

    public static string CategoryHref(string category) => "/?category=" + Uri.EscapeDataString(category);

    public static string ProductHref(string sku) => "/products/" + Uri.EscapeDataString(sku);
}
