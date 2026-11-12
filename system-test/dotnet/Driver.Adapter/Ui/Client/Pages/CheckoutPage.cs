using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;

namespace Driver.Adapter.Ui.Client.Pages;

/// <summary>
/// The checkout `/checkout` (UC-009): shipping address, optional coupon and the order review. The server-rendered UIs
/// post a form and render the checkout again, with an error, when nothing was placed; the single-page UIs stay in
/// place. The address errors are plain messages ("City is required") under the notification's message, and a rejected
/// line is "SKU: reason", so they are read as they are shown rather than parsed as <c>field: message</c>.
/// </summary>
public class CheckoutPage : BasePage
{
    private const string PlaceOrderSelector = "button[aria-label='Place Checkout Order']";

    /// <summary>The checkout has loaded (the single-page UIs load the profile first).</summary>
    public const string ReadySelector = PlaceOrderSelector;

    private const string ReviewLineSelector = "[aria-label='Order Review'] li[aria-label^='Review Line ']";
    private const string ReviewLineLabelPrefix = "Review Line ";
    private const string ReviewQuantitySelectorTemplate = "[aria-label='Review Quantity {0}']";
    private const string ReviewLineTotalSelectorTemplate = "[aria-label='Review Line Total {0}']";
    private const string ReviewSubtotalSelector = "[aria-label='Review Subtotal']";
    private const string TaxCountrySelector = "[aria-label='Tax Country']";
    private const string EstimatedTaxRateSelector = "[aria-label='Estimated Tax Rate']";
    private const string ReviewNoteSelector = "p:has-text('Discount and tax are worked out per order')";
    private const string UseSavedAddressSelector = "input[aria-label='Use Saved Address']";
    private const string SavedAddressSelector = "[aria-label='Saved Address']";
    private const string ShipToDifferentAddressSelector = "input[aria-label='Ship to a Different Address']";
    private const string FirstNameSelector = "input[aria-label='Shipping First Name']";
    private const string LastNameSelector = "input[aria-label='Shipping Last Name']";
    private const string StreetSelector = "input[aria-label='Shipping Street']";
    private const string PostalCodeSelector = "input[aria-label='Shipping Postal Code']";
    private const string CitySelector = "input[aria-label='Shipping City']";
    private const string CountrySelector = "select[aria-label='Shipping Country']";
    /// <summary>"Save as my default shipping address", offered on a first order only.</summary>
    private const string SaveAsDefaultSelector = "input[aria-label='Save as Default Address']";
    private const string CouponCodeSelector = "input[aria-label='Checkout Coupon Code']";
    private const string ErrorSelector = "[role='alert'].notification.error";
    private const string ErrorMessageSelector = ErrorSelector + " .error-message";
    private const string ErrorDetailSelector = ErrorSelector + " .field-error";

    public CheckoutPage(PageClient pageClient) : base(pageClient)
    {
    }

    public Task WaitForLoadedAsync() => PageClient.WaitForVisibleAsync(ReadySelector);

    /// <summary>Chooses "Ship to a different address" (offered only with a saved address) and enters this one.</summary>
    public async Task InputShippingAddressAsync(ShippingAddress address)
    {
        if (!await PageClient.IsHiddenAsync(ShipToDifferentAddressSelector))
        {
            await PageClient.ClickAsync(ShipToDifferentAddressSelector);
        }
        await PageClient.FillAsync(FirstNameSelector, address.FirstName);
        await PageClient.FillAsync(LastNameSelector, address.LastName);
        await PageClient.FillAsync(StreetSelector, address.Street);
        await PageClient.FillAsync(PostalCodeSelector, address.PostalCode);
        await PageClient.FillAsync(CitySelector, address.City);
        await PageClient.SelectOptionByValueAddingIfMissingAsync(CountrySelector, address.Country ?? string.Empty);
    }

    /// <summary>Ticks or unticks "Save as my default shipping address" (offered on a first order only).</summary>
    public Task SetSaveAsDefaultAsync(bool saveAsDefault) => PageClient.SetCheckedAsync(SaveAsDefaultSelector, saveAsDefault);

    public Task InputCouponCodeAsync(string couponCode) => PageClient.FillAsync(CouponCodeSelector, couponCode);

    /// <summary>
    /// Places the order and waits for its outcome: the confirmation, or the checkout with an error. The checkout shows
    /// no error before the order is placed, so either one is the outcome.
    /// </summary>
    public async Task ClickPlaceOrderAsync()
    {
        await PageClient.ClickAsync(PlaceOrderSelector);
        await PageClient.WaitForVisibleAsync(CheckoutConfirmationPage.ReadySelector + ", " + ErrorSelector);
    }

    public async Task<bool> IsShownAsync() => !await PageClient.IsHiddenAsync(ReadySelector);

    public Task<string> ReadTaxCountryAsync() => ReadTextAsync(TaxCountrySelector);

    public Task<string> ReadEstimatedTaxRateAsync() => ReadTextAsync(EstimatedTaxRateSelector);

    /// <summary>The checkout as shown now.</summary>
    public async Task ReadIntoAsync(CheckOutResponse response)
    {
        await WaitForLoadedAsync();
        var lines = new List<CheckOutResponse.ReviewLineDto>();
        foreach (var label in await PageClient.ReadAllAttributesNoWaitAsync(ReviewLineSelector, "aria-label"))
        {
            var sku = label![ReviewLineLabelPrefix.Length..];
            lines.Add(new CheckOutResponse.ReviewLineDto(
                sku,
                int.Parse(await ReadTextAsync(string.Format(ReviewQuantitySelectorTemplate, sku))),
                await ReadTextAsync(string.Format(ReviewLineTotalSelectorTemplate, sku))));
        }
        response.ReviewLines = lines;
        response.ReviewSubtotal = await ReadTextAsync(ReviewSubtotalSelector);
        response.TaxCountry = await ReadTaxCountryAsync();
        response.EstimatedTaxRate = await ReadEstimatedTaxRateAsync();
        response.ReviewNote = await PageClient.IsHiddenAsync(ReviewNoteSelector) ? null : await ReadTextAsync(ReviewNoteSelector);
        var hasSaved = !await PageClient.IsHiddenAsync(UseSavedAddressSelector);
        response.SavedAddressSelected = hasSaved && await PageClient.IsCheckedAsync(UseSavedAddressSelector);
        response.SavedAddress = hasSaved ? await ReadTextAsync(SavedAddressSelector) : null;
        // The form can be in the page but hidden (a server-rendered page hides it while the saved address is chosen).
        var formOpen = await PageClient.IsShownNowAsync(FirstNameSelector);
        response.AddressFormOpen = formOpen;
        response.ShippingFirstName = formOpen ? await PageClient.ReadInputValueAsync(FirstNameSelector) : null;
        response.ShippingLastName = formOpen ? await PageClient.ReadInputValueAsync(LastNameSelector) : null;
        response.SaveAsDefaultTicked = !await PageClient.IsHiddenAsync(SaveAsDefaultSelector)
            && await PageClient.IsCheckedAsync(SaveAsDefaultSelector);
        if (await PageClient.IsHiddenAsync(ErrorSelector))
        {
            response.MessageDetails = [];
        }
        else
        {
            response.Message = await ReadTextAsync(ErrorMessageSelector);
            response.MessageDetails = (await PageClient.ReadAllTextContentsNoWaitAsync(ErrorDetailSelector))
                .Select(detail => detail.Trim())
                .ToList();
        }
    }

    private async Task<string> ReadTextAsync(string selector) => (await PageClient.ReadTextContentAsync(selector)).Trim();
}
