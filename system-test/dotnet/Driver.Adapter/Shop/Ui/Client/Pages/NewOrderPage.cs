using System.Text.RegularExpressions;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.Shared.Client.Playwright;



namespace Driver.Adapter.Shop.Ui.Client.Pages;



public class NewOrderPage : BasePage

{

    private const string SkuInputSelector = "[aria-label=\"SKU\"]";

    private const string QuantityInputSelector = "[aria-label=\"Quantity\"]";

    private const string CountryInputSelector = "[aria-label=\"Country\"]";

    private const string CouponCodeInputSelector = "[aria-label=\"Coupon Code\"]";

    private const string PlaceOrderButtonSelector = "[aria-label=\"Place Order\"]";

    private const string OrderNumberRegex = @"Success! Order has been created with Order Number ([\w-]+)";

    private const int OrderNumberMatcherGroup = 1;

    private const string OrderNumberNotFoundError = "Could not find order number";



    public NewOrderPage(PageClient pageClient) : base(pageClient)

    {

    }



    public async Task InputSkuAsync(string? sku)

    {

        await PageClient.FillAsync(SkuInputSelector, sku);

    }



    public async Task InputQuantityAsync(string? quantity)

    {

        await PageClient.FillAsync(QuantityInputSelector, quantity);

    }



    public async Task InputCountryAsync(string? country)

    {

        await PageClient.FillAsync(CountryInputSelector, country);

    }



    public async Task InputCouponCodeAsync(string? couponCode)

    {

        await PageClient.FillAsync(CouponCodeInputSelector, couponCode);

    }



    public async Task ClickPlaceOrderAsync()

    {

        await PageClient.ClickAsync(PlaceOrderButtonSelector);

    }



    public static string GetOrderNumber(string successMessageText)

    {

        var pattern = new Regex(OrderNumberRegex);

        var match = pattern.Match(successMessageText);



        if (!match.Success)

        {

            throw new InvalidOperationException(OrderNumberNotFoundError);

        }



        return match.Groups[OrderNumberMatcherGroup].Value;

    }

}





