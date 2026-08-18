using Driver.Adapter.Shared.Client.Playwright;
using Driver.Port.Dtos;
using Common.Domain;

namespace Driver.Adapter.Ui.Client.Pages;

public class OrderDetailsPage : BasePage
{
    private const string OrderNumberOutputSelector = "[aria-label='Display Order Number']";
    private const string OrderTimestampOutputSelector = "[aria-label='Display Order Timestamp']";
    private const string SkuOutputSelector = "[aria-label='Display SKU']";
    private const string CountryOutputSelector = "[aria-label='Display Country']";
    private const string QuantityOutputSelector = "[aria-label='Display Quantity']";
    private const string UnitPriceOutputSelector = "[aria-label='Display Unit Price']";
    private const string BasePriceOutputSelector = "[aria-label='Display Base Price']";
    private const string SubtotalPriceOutputSelector = "[aria-label='Display Subtotal Price']";
    private const string DiscountRateOutputSelector = "[aria-label='Display Discount Rate']";
    private const string DiscountAmountOutputSelector = "[aria-label='Display Discount Amount']";
    private const string TaxRateOutputSelector = "[aria-label='Display Tax Rate']";
    private const string TaxAmountOutputSelector = "[aria-label='Display Tax Amount']";
    private const string TotalPriceOutputSelector = "[aria-label='Display Total Price']";
    private const string StatusOutputSelector = "[aria-label='Display Status']";
    private const string AppliedCouponOutputSelector = "[aria-label='Display Applied Coupon']";
    private const string CancelOrderOutputSelector = "[aria-label='Cancel Order']";
    private const string DeliverOrderOutputSelector = "[aria-label='Deliver Order']";

    // Display text constants
    private const string TextNone = "None";
    private const string DollarSymbol = "$";
    private const string PercentSymbol = "%";

    // Enum parsing constants
    private const bool IgnoreCase = true;

    public OrderDetailsPage(PageClient pageClient) : base(pageClient)
    {
    }

    public async Task<bool> IsLoadedSuccessfullyAsync()
    {
        return await PageClient.IsVisibleAsync(OrderNumberOutputSelector);
    }

    public async Task<string> GetOrderNumberAsync()
    {
        return await PageClient.ReadTextContentAsync(OrderNumberOutputSelector);
    }

    public async Task<DateTimeOffset> GetOrderTimestampAsync()
    {
        var textContent = await PageClient.ReadTextContentAsync(OrderTimestampOutputSelector);
        return DateTimeOffset.Parse(textContent, System.Globalization.CultureInfo.InvariantCulture);
    }

    public async Task<string> GetSkuAsync()
    {
        return await PageClient.ReadTextContentAsync(SkuOutputSelector);
    }

    public async Task<string> GetCountryAsync()
    {
        return await PageClient.ReadTextContentAsync(CountryOutputSelector);
    }

    public async Task<int> GetQuantityAsync()
    {
        var textContent = await PageClient.ReadTextContentAsync(QuantityOutputSelector);
        return int.Parse(textContent);
    }

    public async Task<decimal> GetUnitPriceAsync()
    {
        return await ReadTextMoneyAsync(UnitPriceOutputSelector);
    }

    public async Task<decimal> GetBasePriceAsync()
    {
        return await ReadTextMoneyAsync(BasePriceOutputSelector);
    }

    public async Task<decimal> GetDiscountRateAsync()
    {
        return await ReadTextPercentageAsync(DiscountRateOutputSelector);
    }

    public async Task<decimal> GetDiscountAmountAsync()
    {
        return await ReadTextMoneyAsync(DiscountAmountOutputSelector);
    }

    public async Task<decimal> GetSubtotalPriceAsync()
    {
        return await ReadTextMoneyAsync(SubtotalPriceOutputSelector);
    }

    public async Task<decimal> GetTaxRateAsync()
    {
        return await ReadTextPercentageAsync(TaxRateOutputSelector);
    }

    public async Task<decimal> GetTaxAmountAsync()
    {
        return await ReadTextMoneyAsync(TaxAmountOutputSelector);
    }

    public async Task<decimal> GetTotalPriceAsync()
    {
        return await ReadTextMoneyAsync(TotalPriceOutputSelector);
    }

    public async Task<OrderStatus> GetStatusAsync()
    {
        var status = await PageClient.ReadTextContentAsync(StatusOutputSelector);
        return Enum.Parse<OrderStatus>(status, IgnoreCase);
    }

    public async Task<string?> GetAppliedCouponAsync()
    {
        var coupon = await PageClient.ReadTextContentAsync(AppliedCouponOutputSelector);
        return TextNone.Equals(coupon) ? null : coupon;
    }

    public async Task ClickCancelOrderAsync()
    {
        await PageClient.ClickAsync(CancelOrderOutputSelector);
    }

    public async Task ClickDeliverOrderAsync()
    {
        await PageClient.ClickAsync(DeliverOrderOutputSelector);
    }

    public async Task<bool> IsCancelButtonHiddenAsync()
    {
        return await PageClient.IsHiddenAsync(CancelOrderOutputSelector);
    }

    private async Task<decimal> ReadTextMoneyAsync(string selector)
    {
        var textContent = await PageClient.ReadTextContentAsync(selector);
        var cleaned = textContent.Replace(DollarSymbol, "").Trim();
        return decimal.Parse(cleaned);
    }

    private async Task<decimal> ReadTextPercentageAsync(string selector)
    {
        var textContent = await PageClient.ReadTextContentAsync(selector);
        var cleaned = textContent.Replace(PercentSymbol, "").Trim();
        var value = decimal.Parse(cleaned);
        return value / 100;
    }
}
