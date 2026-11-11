using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>The order confirmation `/checkout/confirmation` (UC-009): the orders just placed and the rejected lines.</summary>
public class CheckoutConfirmationPage : BasePage
{
    private const string ConfirmedOrdersSelector = "ul[aria-label='Confirmed Orders']";
    private const string RejectedLinesSelector = "[aria-label='Rejected Lines']";
    private const string NoRecentOrderSelector = "h1:has-text('No recent order')";

    /// <summary>Exactly one of the confirmed orders or "No recent order" shows once the confirmation has loaded.</summary>
    public const string ReadySelector = ConfirmedOrdersSelector + ", " + NoRecentOrderSelector;

    private const string ConfirmedOrderSelector = ConfirmedOrdersSelector + " a[aria-label^='Confirmed Order ']";
    private const string RejectedLineSelector = RejectedLinesSelector + " li[aria-label^='Rejected Line ']";
    private const string RejectedLineLabelPrefix = "Rejected Line ";
    private const string RejectionReasonSelectorTemplate = "[aria-label='Rejection Reason {0}']";
    private const string ThanksSelector = "h1:has-text('Thank you')";
    private const string ViewMyOrdersSelector = "a:text-is('View my orders')";
    private const string ContinueShoppingSelector = "a:text-is('Continue shopping')";

    public CheckoutConfirmationPage(PageClient pageClient) : base(pageClient)
    {
    }

    public async Task<bool> IsShownAsync() => !await PageClient.IsHiddenAsync(ReadySelector);

    public async Task ReadIntoAsync(CheckOutResponse response)
    {
        await PageClient.WaitForVisibleAsync(ReadySelector);
        response.Thanks = await PageClient.IsHiddenAsync(ThanksSelector)
            ? null
            : (await PageClient.ReadTextContentAsync(ThanksSelector)).Trim();
        var numbers = await PageClient.ReadAllTextContentsNoWaitAsync(ConfirmedOrderSelector);
        var links = await PageClient.ReadAllAttributesNoWaitAsync(ConfirmedOrderSelector, "href");
        response.ConfirmedOrders = numbers
            .Select((number, i) => new CheckOutResponse.ConfirmedOrderDto(number.Trim(), links[i]))
            .ToList();
        var rejected = new List<CheckOutResponse.RejectedLineDto>();
        foreach (var label in await PageClient.ReadAllAttributesNoWaitAsync(RejectedLineSelector, "aria-label"))
        {
            var sku = label![RejectedLineLabelPrefix.Length..];
            var reason = (await PageClient.ReadTextContentAsync(string.Format(RejectionReasonSelectorTemplate, sku))).Trim();
            rejected.Add(new CheckOutResponse.RejectedLineDto(sku, reason));
        }
        response.RejectedLines = rejected;
        response.ViewMyOrdersOffered = !await PageClient.IsHiddenAsync(ViewMyOrdersSelector);
        response.ContinueShoppingOffered = !await PageClient.IsHiddenAsync(ContinueShoppingSelector);
    }
}
