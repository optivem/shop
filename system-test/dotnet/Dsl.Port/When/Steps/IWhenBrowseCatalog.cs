using Dsl.Port.When.Steps.Base;

namespace Dsl.Port.When.Steps;

/// <summary>
/// Browses the catalog as a visitor (or as the logged-in user). The steps run in this order: follow a header
/// category link, search, pick a category, tick brands, sort; then reopen the page address or clear the filters.
/// </summary>
public interface IWhenBrowseCatalog : IWhenStep
{
    IWhenBrowseCatalog WithHeaderCategoryLink(string category);

    IWhenBrowseCatalog WithSearch(string term);

    IWhenBrowseCatalog WithCategory(string category);

    IWhenBrowseCatalog WithBrands(params string[] brands);

    /// <summary>The sort order's label, e.g. "Price: high to low".</summary>
    IWhenBrowseCatalog WithSort(string sort);

    /// <summary>After the other steps, opens the same page address again.</summary>
    IWhenBrowseCatalog WithPageAddressReopened();

    /// <summary>After the other steps, clears the filters.</summary>
    IWhenBrowseCatalog WithFiltersCleared();
}
