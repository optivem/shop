using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>The cart `/cart` (UC-008), read for its lines only.</summary>
public class CartPage : BasePage
{
    private const string CartLinesSelector = "table[aria-label='Cart Lines']";
    private const string EmptyCartSelector = "p.lead:has-text('Your cart is empty.')";
    public const string ReadySelector = CartLinesSelector + ", " + EmptyCartSelector;
    private const string CartLineSelector = CartLinesSelector + " tr[aria-label^='Cart Line ']";
    private const string CartLineLabelPrefix = "Cart Line ";
    private const string QuantitySelectorTemplate = "input[aria-label='Quantity for {0}']";

    public CartPage(PageClient pageClient) : base(pageClient)
    {
    }

    public async Task<List<ViewProductResponse.CartLineDto>> ReadLinesAsync()
    {
        await PageClient.WaitForVisibleAsync(ReadySelector);
        var skus = (await PageClient.ReadAllAttributesNoWaitAsync(CartLineSelector, "aria-label"))
            .Select(label => label![CartLineLabelPrefix.Length..])
            .ToList();
        var lines = new List<ViewProductResponse.CartLineDto>();
        foreach (var sku in skus)
        {
            var quantity = await PageClient.ReadInputValueAsync(string.Format(QuantitySelectorTemplate, sku));
            lines.Add(new ViewProductResponse.CartLineDto(sku, int.Parse(quantity.Trim())));
        }
        return lines;
    }
}
