using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>The catalog on the home page `/` (UC-006): header search and category links, filters, sort and the product grid.</summary>
public class CatalogPage : BasePage
{
    private const string CatalogSelector = "section[aria-label='Catalog']";
    private const string ProductListSelector = CatalogSelector + " [aria-label='Product List']";
    private const string ClearFiltersSelector = CatalogSelector + " [aria-label='Clear Filters']";
    private const string UnavailableSelector = CatalogSelector + " .alert-danger";
    private const string NoMatchSelector = CatalogSelector + " p.lead";

    /// <summary>Exactly one of these shows once the catalog has loaded: products, the no-match notice or the ERP outage.</summary>
    public const string ReadySelector = CatalogSelector
        + " :is([aria-label='Product List'], [aria-label='Clear Filters'], .alert-danger)";

    private const string ProductCardSelector = ProductListSelector + " article[aria-label^='Product ']";
    private const string ProductCardLabelPrefix = "Product ";
    private const string ProductCountSelector = CatalogSelector + " [aria-label='Product Count']";
    private const string SortSelector = CatalogSelector + " select[aria-label='Sort']";
    private const string CategoryFilterSelector = "fieldset[aria-label='Category Filter']";
    private const string BrandFilterSelector = "fieldset[aria-label='Brand Filter']";
    private const string CheckedOptionSelector = " .form-check:has(input:checked) label";
    private const string SearchInputSelector = "input[aria-label='Search Products']";
    private const string SearchButtonSelector = "button[aria-label='Search']";
    private const string HeaderCategoryLinkSelectorTemplate = "nav[aria-label='Store navigation'] a:text-is('{0}')";
    private const string OptionLabelSelectorTemplate = "{0} label:text-is('{1}')";
    private const string SearchParameter = "q=";

    public CatalogPage(PageClient pageClient) : base(pageClient)
    {
    }

    public Task WaitForLoadedAsync() => PageClient.WaitForVisibleAsync(ReadySelector);

    public Task ClickHeaderCategoryAsync(string category)
        => ChangeViewAsync(() => PageClient.ClickAsync(string.Format(HeaderCategoryLinkSelectorTemplate, category)));

    /// <summary>An empty search on an unsearched catalog shows the same page address, so there is no change to wait for.</summary>
    public async Task SearchAsync(string term)
    {
        var sameAddress = string.IsNullOrWhiteSpace(term) && !PageClient.CurrentUrl().Contains(SearchParameter);
        await PageClient.FillAsync(SearchInputSelector, term);
        if (sameAddress)
        {
            await PageClient.ClickAsync(SearchButtonSelector);
            await WaitForLoadedAsync();
            return;
        }
        await ChangeViewAsync(() => PageClient.ClickAsync(SearchButtonSelector));
    }

    public async Task SelectCategoryAsync(string category)
    {
        if (category == await ReadSelectedCategoryAsync())
        {
            return;
        }
        await ChangeViewAsync(() => PageClient.ClickAsync(string.Format(OptionLabelSelectorTemplate, CategoryFilterSelector, category)));
    }

    public async Task TickBrandAsync(string brand)
    {
        if ((await ReadTickedBrandsAsync()).Contains(brand))
        {
            return;
        }
        await ChangeViewAsync(() => PageClient.ClickAsync(string.Format(OptionLabelSelectorTemplate, BrandFilterSelector, brand)));
    }

    public async Task SortByAsync(string sortLabel)
    {
        if (sortLabel == await PageClient.ReadSelectedOptionLabelAsync(SortSelector))
        {
            return;
        }
        await ChangeViewAsync(() => PageClient.SelectOptionByLabelAsync(SortSelector, sortLabel));
    }

    public async Task ReopenPageAddressAsync()
    {
        await PageClient.ReopenCurrentUrlAsync();
        await WaitForLoadedAsync();
    }

    public Task ClickClearFiltersAsync() => ChangeViewAsync(() => PageClient.ClickAsync(ClearFiltersSelector));

    public async Task<BrowseCatalogResponse> ReadAsync()
    {
        await WaitForLoadedAsync();
        var skus = (await PageClient.ReadAllAttributesNoWaitAsync(ProductCardSelector, "aria-label"))
            .Select(label => label![ProductCardLabelPrefix.Length..])
            .ToList();
        var titles = Trimmed(await PageClient.ReadAllTextContentsNoWaitAsync(ProductCardSelector + " [aria-label='Product Title']"));
        var brands = Trimmed(await PageClient.ReadAllTextContentsNoWaitAsync(ProductCardSelector + " [aria-label='Product Brand']"));
        var prices = Trimmed(await PageClient.ReadAllTextContentsNoWaitAsync(ProductCardSelector + " [aria-label='Display Price']"));

        var products = skus
            .Select((sku, i) => new BrowseCatalogResponse.ProductDto
            {
                Sku = sku,
                Title = titles[i],
                Brand = brands[i],
                Price = prices[i],
            })
            .ToList();

        return new BrowseCatalogResponse
        {
            Products = products,
            ProductCount = await ReadProductCountAsync(),
            Categories = Trimmed(await PageClient.ReadAllTextContentsNoWaitAsync(CategoryFilterSelector + " label")),
            SelectedCategory = await ReadSelectedCategoryAsync(),
            Brands = Trimmed(await PageClient.ReadAllTextContentsNoWaitAsync(BrandFilterSelector + " label")),
            TickedBrands = await ReadTickedBrandsAsync(),
            Sort = await PageClient.ReadSelectedOptionLabelAsync(SortSelector),
            Message = await ReadMessageAsync(),
            ClearFiltersOffered = !await PageClient.IsHiddenAsync(ClearFiltersSelector),
        };
    }

    /// <summary>Runs an action that changes the view, then waits for the new page address and the re-rendered catalog.</summary>
    private async Task ChangeViewAsync(Func<Task> action)
    {
        var previousUrl = PageClient.CurrentUrl();
        await action();
        await PageClient.WaitForUrlChangeAsync(previousUrl);
        await WaitForLoadedAsync();
    }

    private async Task<int> ReadProductCountAsync()
    {
        var text = (await PageClient.ReadTextContentAsync(ProductCountSelector)).Trim();
        return int.Parse(text.Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries)[0]);
    }

    private async Task<string?> ReadSelectedCategoryAsync()
    {
        var selected = Trimmed(await PageClient.ReadAllTextContentsNoWaitAsync(CategoryFilterSelector + CheckedOptionSelector));
        return selected.Count == 0 ? null : selected[0];
    }

    private async Task<List<string>> ReadTickedBrandsAsync()
        => Trimmed(await PageClient.ReadAllTextContentsNoWaitAsync(BrandFilterSelector + CheckedOptionSelector));

    private async Task<string?> ReadMessageAsync()
    {
        if (!await PageClient.IsHiddenAsync(UnavailableSelector))
        {
            return (await PageClient.ReadTextContentAsync(UnavailableSelector)).Trim();
        }
        if (!await PageClient.IsHiddenAsync(NoMatchSelector))
        {
            return (await PageClient.ReadTextContentAsync(NoMatchSelector)).Trim();
        }
        return null;
    }

    private static List<string> Trimmed(IEnumerable<string> values) => values.Select(value => value.Trim()).ToList();
}
