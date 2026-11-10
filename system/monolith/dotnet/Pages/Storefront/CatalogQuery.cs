using System.Net;
using MyCompany.MyShop.Monolith.Core.Dtos;

namespace MyCompany.MyShop.Monolith.Pages.Storefront;

/// <summary>One choice of the catalog's Sort select.</summary>
public sealed record SortOption(string Value, string Label);

/// <summary>
/// Catalog filtering and sorting (UC-006 BR-2..BR-7). The catalog is small and the ERP has no query API, so
/// filtering happens here over the full list, and the query lives in the page address.
/// </summary>
/// <param name="Q">search term over title, brand and description, case-insensitive</param>
/// <param name="Category">one category, or "" for All</param>
/// <param name="Brands">any number of brands; none means every brand</param>
/// <param name="Sort">one of <see cref="SortOptions"/></param>
public sealed record CatalogQuery(string Q, string Category, IReadOnlyList<string> Brands, string Sort)
{
    public const string Featured = "featured";

    public static readonly IReadOnlyList<SortOption> SortOptions =
    [
        new(Featured, "Featured"),
        new("price-asc", "Price: low to high"),
        new("price-desc", "Price: high to low"),
        new("name", "Name: A to Z"),
    ];

    public static CatalogQuery Parse(string? q, string? category, IEnumerable<string?>? brands, string? sort)
    {
        var knownSort = SortOptions.Any(option => option.Value == sort) ? sort! : Featured;
        var tickedBrands = (brands ?? []).Where(brand => !string.IsNullOrEmpty(brand)).Select(brand => brand!).ToList();
        return new CatalogQuery(q ?? string.Empty, category ?? string.Empty, tickedBrands, knownSort);
    }

    /// <summary>
    /// The filter form sends every field, defaults included (an empty value, or the Featured sort): such a request is
    /// redirected to the short address of the same view.
    /// </summary>
    public static bool HasDefaultParams(IQueryCollection query) =>
        query.Any(param => param.Value.Any(string.IsNullOrEmpty))
        || (query.TryGetValue("sort", out var sort) && sort.Contains(Featured));

    /// <summary>The page address for this query; defaults are left out so the plain catalog is just "/".</summary>
    public string Path()
    {
        var parameters = new List<string>();
        if (!string.IsNullOrWhiteSpace(Q))
        {
            parameters.Add("q=" + WebUtility.UrlEncode(Q.Trim()));
        }

        if (Category.Length > 0)
        {
            parameters.Add("category=" + WebUtility.UrlEncode(Category));
        }

        parameters.AddRange(Brands.Select(brand => "brand=" + WebUtility.UrlEncode(brand)));
        if (Sort != Featured)
        {
            parameters.Add("sort=" + WebUtility.UrlEncode(Sort));
        }

        return parameters.Count == 0 ? "/" : "/?" + string.Join('&', parameters);
    }

    public bool IsBrandTicked(string brand) => Brands.Contains(brand);

    /// <summary>The products matching every filter, in the chosen order (Featured keeps the ERP order).</summary>
    public IReadOnlyList<ViewProductDetailsResponse> Apply(IEnumerable<ViewProductDetailsResponse> products)
    {
        var filtered = products.Where(Matches);
        return (Sort switch
        {
            "price-asc" => filtered.OrderBy(product => product.Price),
            "price-desc" => filtered.OrderByDescending(product => product.Price),
            "name" => filtered.OrderBy(product => product.Title ?? string.Empty, StringComparer.OrdinalIgnoreCase),
            _ => filtered,
        }).ToList();
    }

    public static IReadOnlyList<string> CategoriesOf(IEnumerable<ViewProductDetailsResponse> products) =>
        Distinct(products.Select(product => product.Category));

    public static IReadOnlyList<string> BrandsOf(IEnumerable<ViewProductDetailsResponse> products) =>
        Distinct(products.Select(product => product.Brand));

    /// <summary>Distinct non-empty values, in first-seen (ERP) order.</summary>
    private static List<string> Distinct(IEnumerable<string?> values) =>
        values.Where(value => !string.IsNullOrEmpty(value)).Select(value => value!).Distinct().ToList();

    private bool Matches(ViewProductDetailsResponse product)
    {
        var term = Q.Trim();
        var matchesTerm = term.Length == 0 || Contains(product.Title, term) || Contains(product.Brand, term)
            || Contains(product.Description, term);
        var matchesCategory = Category.Length == 0 || Category == product.Category;
        var matchesBrand = Brands.Count == 0 || (product.Brand != null && Brands.Contains(product.Brand));
        return matchesTerm && matchesCategory && matchesBrand;
    }

    private static bool Contains(string? text, string term) =>
        text != null && text.Contains(term, StringComparison.OrdinalIgnoreCase);
}
