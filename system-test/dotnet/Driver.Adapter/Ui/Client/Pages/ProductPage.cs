using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>A product's page `/products/{sku}` (UC-007): its details, the quantity stepper and "Add to cart".</summary>
public class ProductPage : BasePage
{
    private const string TitleSelector = "h1[aria-label='Product Title']";
    private const string BackToCatalogSelector = "[aria-label='Back to Catalog']";
    private const string NotFoundHeadingSelector = ":has(> " + BackToCatalogSelector + ") > h1";
    private const string UnavailableSelector = "main .alert-danger";

    /// <summary>Exactly one of these shows once the product page has loaded: the product, product not found or the ERP outage.</summary>
    public const string ReadySelector = ":is(" + TitleSelector + ", " + BackToCatalogSelector + ", " + UnavailableSelector + ")";

    private const string BrandSelector = "[aria-label='Product Brand']";
    private const string CategorySelector = "[aria-label='Product Category']";
    private const string SkuSelector = "[aria-label='Product SKU']";
    private const string PriceSelector = "[aria-label='Display Price']";
    private const string DescriptionSelector = "[aria-label='Product Description']";
    private const string BreadcrumbSelector = "nav[aria-label='breadcrumb'] .breadcrumb-item";
    private const string QuantitySelector = "input[aria-label='Product Quantity']";
    private const string IncreaseSelector = "button[aria-label='Increase Quantity']";
    private const string DecreaseSelector = "button[aria-label='Decrease Quantity']";
    private const string AddToCartSelector = "button[aria-label='Add to Cart']";
    private const string CartItemCountSelector = "[aria-label='Cart Item Count']";
    private const string ProductPathPrefix = "/products/";

    private string? _notificationMessage;

    public ProductPage(PageClient pageClient) : base(pageClient)
    {
    }

    public Task WaitForLoadedAsync() => PageClient.WaitForVisibleAsync(ReadySelector);

    public Task InputQuantityAsync(string? quantity) => PageClient.FillAsync(QuantitySelector, quantity);

    public Task ClickIncreaseQuantityAsync() => PageClient.ClickAsync(IncreaseSelector);

    public Task ClickDecreaseQuantityAsync() => PageClient.ClickAsync(DecreaseSelector);

    /// <summary>Adds to the cart and waits for the confirmation or the error, which <see cref="ReadAsync"/> reports as the message.</summary>
    public async Task ClickAddToCartAsync()
    {
        await PageClient.ClickAsync(AddToCartSelector);
        var result = await GetResultAsync();
        var message = result.IsSuccess ? result.Value : result.Error.Message;
        _notificationMessage = message?.Trim();
        await WaitForLoadedAsync();
    }

    public async Task<CatalogPage> ClickBackToCatalogAsync()
    {
        var previousUrl = PageClient.CurrentUrl();
        await PageClient.ClickAsync(BackToCatalogSelector);
        await PageClient.WaitForUrlChangeAsync(previousUrl);
        var catalogPage = new CatalogPage(PageClient);
        await catalogPage.WaitForLoadedAsync();
        return catalogPage;
    }

    public async Task<ViewProductResponse> ReadAsync()
    {
        await WaitForLoadedAsync();
        var response = new ViewProductResponse
        {
            Message = await ReadMessageAsync(),
            BackToCatalogOffered = !await PageClient.IsHiddenAsync(BackToCatalogSelector),
            AddToCartOffered = !await PageClient.IsHiddenAsync(AddToCartSelector),
            CartItemCount = await ReadCartItemCountAsync(),
            PageSku = ReadPageSku(),
            Breadcrumbs = [],
        };
        if (await PageClient.IsHiddenAsync(TitleSelector))
        {
            return response;
        }

        response.Title = await ReadTextAsync(TitleSelector);
        response.Brand = await ReadTextAsync(BrandSelector);
        response.Category = await ReadTextAsync(CategorySelector);
        response.Sku = await ReadTextAsync(SkuSelector);
        response.Price = await ReadTextAsync(PriceSelector);
        response.Description = await ReadTextAsync(DescriptionSelector);
        response.Breadcrumbs = (await PageClient.ReadAllTextContentsNoWaitAsync(BreadcrumbSelector)).Select(item => item.Trim()).ToList();
        response.Quantity = await PageClient.ReadInputValueAsync(QuantitySelector);
        return response;
    }

    private async Task<string?> ReadMessageAsync()
    {
        if (_notificationMessage != null)
        {
            return _notificationMessage;
        }
        if (!await PageClient.IsHiddenAsync(NotFoundHeadingSelector))
        {
            return await ReadTextAsync(NotFoundHeadingSelector);
        }
        if (!await PageClient.IsHiddenAsync(UnavailableSelector))
        {
            return await ReadTextAsync(UnavailableSelector);
        }
        return null;
    }

    private async Task<int> ReadCartItemCountAsync()
    {
        if (await PageClient.IsHiddenAsync(CartItemCountSelector))
        {
            return 0;
        }
        return int.Parse(await ReadTextAsync(CartItemCountSelector));
    }

    private string? ReadPageSku()
    {
        var path = new Uri(PageClient.CurrentUrl()).AbsolutePath;
        return path.StartsWith(ProductPathPrefix, StringComparison.Ordinal)
            ? Uri.UnescapeDataString(path[ProductPathPrefix.Length..])
            : null;
    }

    private async Task<string> ReadTextAsync(string selector) => (await PageClient.ReadTextContentAsync(selector)).Trim();
}
