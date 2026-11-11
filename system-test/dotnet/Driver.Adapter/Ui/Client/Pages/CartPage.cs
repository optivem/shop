using System.Text.RegularExpressions;
using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>
/// The cart `/cart` (UC-008): its lines, quantity changes, removal and the subtotal. The server-rendered UIs post a
/// form and load the cart again for every change; the single-page UIs change it in place.
/// </summary>
public class CartPage : BasePage
{
    private const string CartLinesSelector = "table[aria-label='Cart Lines']";
    private const string EmptyCartSelector = "p.lead:has-text('Your cart is empty.')";
    private const string UnavailableSelector = "main .alert-danger";

    /// <summary>Exactly one of these shows once the cart has loaded: its lines, the empty cart or the ERP outage.</summary>
    public const string ReadySelector = CartLinesSelector + ", " + EmptyCartSelector + ", " + UnavailableSelector;

    private const string CartLineSelector = CartLinesSelector + " tr[aria-label^='Cart Line ']";
    private const string CartLineLabelPrefix = "Cart Line ";
    private const string QuantitySelectorTemplate = "input[aria-label='Quantity for {0}']";
    private const string TitleSelectorTemplate = "[aria-label='Title {0}']";
    private const string UnitPriceSelectorTemplate = "[aria-label='Unit Price {0}']";
    private const string LineTotalSelectorTemplate = "[aria-label='Line Total {0}']";
    private const string IncreaseSelectorTemplate = "button[aria-label='Increase Quantity for {0}']";
    private const string DecreaseSelectorTemplate = "button[aria-label='Decrease Quantity for {0}']";
    private const string RemoveSelectorTemplate = "button[aria-label='Remove {0}']";
    private const string LineCountSelector = "[aria-label='Cart Line Count']";
    private const string SubtotalSelector = "[aria-label='Cart Subtotal']";
    private const string CheckoutNoteSelector = "p:has-text('Tax and discounts are calculated at checkout')";
    private const string StartShoppingSelector = "a:text-is('Start shopping')";

    /// <summary>A typed quantity is committed when the field loses focus.</summary>
    private const string CommitKey = "Tab";

    public CartPage(PageClient pageClient) : base(pageClient)
    {
    }

    public Task WaitForLoadedAsync() => PageClient.WaitForVisibleAsync(ReadySelector);

    public async Task InputQuantityAsync(string sku, string? quantity)
    {
        var selector = string.Format(QuantitySelectorTemplate, sku);
        await PageClient.FillAsync(selector, quantity);
        await ChangeAsync(selector, () => PageClient.PressAsync(selector, CommitKey));
    }

    public Task ClickIncreaseQuantityAsync(string sku) => ClickAsync(string.Format(IncreaseSelectorTemplate, sku));

    public Task ClickDecreaseQuantityAsync(string sku) => ClickAsync(string.Format(DecreaseSelectorTemplate, sku));

    public Task ClickRemoveAsync(string sku) => ClickAsync(string.Format(RemoveSelectorTemplate, sku));

    public async Task<List<ViewProductResponse.CartLineDto>> ReadLinesAsync()
    {
        await WaitForLoadedAsync();
        var lines = new List<ViewProductResponse.CartLineDto>();
        foreach (var sku in await ReadCartLineSkusAsync())
        {
            lines.Add(new ViewProductResponse.CartLineDto(sku, await ReadQuantityAsync(sku)));
        }
        return lines;
    }

    /// <summary>The cart as shown; <paramref name="message"/> is the action's notification, if it had one.</summary>
    public async Task<ManageCartResponse> ReadAsync(string? message, int? cartItemCount)
    {
        await WaitForLoadedAsync();
        var lines = new List<ManageCartResponse.CartLineDto>();
        foreach (var sku in await ReadCartLineSkusAsync())
        {
            lines.Add(new ManageCartResponse.CartLineDto(
                sku,
                await ReadTextAsync(string.Format(TitleSelectorTemplate, sku)),
                await ReadTextAsync(string.Format(UnitPriceSelectorTemplate, sku)),
                await ReadQuantityAsync(sku),
                await ReadTextAsync(string.Format(LineTotalSelectorTemplate, sku))));
        }
        return new ManageCartResponse
        {
            Message = message ?? await ReadMessageAsync(),
            CartItemCount = cartItemCount ?? await ReadCartItemCountAsync(),
            Lines = lines,
            ItemCount = await ReadItemCountAsync(),
            Subtotal = await PageClient.IsHiddenAsync(SubtotalSelector) ? null : await ReadTextAsync(SubtotalSelector),
            CheckoutNote = await PageClient.IsHiddenAsync(CheckoutNoteSelector) ? null : await ReadTextAsync(CheckoutNoteSelector),
            StartShoppingOffered = !await PageClient.IsHiddenAsync(StartShoppingSelector),
        };
    }

    /// <summary>The notification an action on the cart showed, if any.</summary>
    public Task<string?> ReadNotificationAsync() => ReadShownNotificationAsync();

    private Task ClickAsync(string selector) => ChangeAsync(selector, () => PageClient.ClickAsync(selector));

    /// <summary>Runs a change and waits for the cart to show it: a new document when the control posts a form.</summary>
    private async Task ChangeAsync(string selector, Func<Task> action)
    {
        if (await PageClient.BelongsToFormAsync(selector))
        {
            await PageClient.RunAndWaitForNewDocumentAsync(action);
        }
        else
        {
            await action();
        }
        await WaitForLoadedAsync();
    }

    private async Task<List<string>> ReadCartLineSkusAsync()
        => (await PageClient.ReadAllAttributesNoWaitAsync(CartLineSelector, "aria-label"))
            .Select(label => label![CartLineLabelPrefix.Length..])
            .ToList();

    private async Task<int> ReadQuantityAsync(string sku)
        => int.Parse((await PageClient.ReadInputValueAsync(string.Format(QuantitySelectorTemplate, sku))).Trim());

    /// <summary>"(2 items)" next to the heading.</summary>
    private async Task<int?> ReadItemCountAsync()
    {
        if (await PageClient.IsHiddenAsync(LineCountSelector))
        {
            return null;
        }
        var digits = Regex.Replace(await ReadTextAsync(LineCountSelector), @"\D+", " ").Trim();
        return int.Parse(digits.Split(' ')[0]);
    }

    private async Task<string?> ReadMessageAsync()
    {
        var notification = await ReadShownNotificationAsync();
        if (notification != null)
        {
            return notification;
        }
        if (!await PageClient.IsHiddenAsync(EmptyCartSelector))
        {
            return await ReadTextAsync(EmptyCartSelector);
        }
        if (!await PageClient.IsHiddenAsync(UnavailableSelector))
        {
            return await ReadTextAsync(UnavailableSelector);
        }
        return null;
    }

    private async Task<string> ReadTextAsync(string selector) => (await PageClient.ReadTextContentAsync(selector)).Trim();
}
