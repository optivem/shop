namespace Driver.Port.Dtos;

/// <summary>
/// How the visitor browses the catalog, applied in this order: follow a header category link, search, pick a
/// category, tick brands, sort; then the follow-up, if any. Unset inputs are left untouched. Only the UI has these
/// controls: the API serves the unfiltered list.
/// </summary>
public class BrowseCatalogRequest
{
    /// <summary>Opens the same page address again.</summary>
    public const string ReopenPageAddress = "reopen-page-address";

    /// <summary>Clears the filters.</summary>
    public const string ClearFilters = "clear-filters";

    /// <summary>Separates the ticked brands.</summary>
    public const string BrandSeparator = ",";

    public string? HeaderCategory { get; set; }
    public string? Search { get; set; }
    public string? Category { get; set; }

    /// <summary>The brands to tick, joined with <see cref="BrandSeparator"/>.</summary>
    public string? Brands { get; set; }

    /// <summary>The sort order's label, e.g. "Price: low to high".</summary>
    public string? Sort { get; set; }

    /// <summary><see cref="ReopenPageAddress"/> or <see cref="ClearFilters"/>, run after the other inputs.</summary>
    public string? FollowUp { get; set; }
}
