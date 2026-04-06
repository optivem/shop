using System.Text;
using System.Text.RegularExpressions;
using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod03.E2eTests.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod03.E2eTests;

public class PlaceOrderPositiveUiTest : BaseE2eTest
{
    protected override async Task SetShopRawAsync()
    {
        await SetUpShopBrowserAsync();
    }

    [Fact]
    public async Task ShouldPlaceOrderForValidInput()
    {
        var sku = CreateUniqueSku(Defaults.SKU);
        var createProductJson = $$"""{"id":"{{sku}}","title":"Test Product","description":"Test Description","category":"Test Category","brand":"Test Brand","price":"20.00"}""";

        var createProductUri = new Uri(_configuration.ErpBaseUrl + "/api/products");
        var createProductContent = new StringContent(createProductJson, Encoding.UTF8, "application/json");
        var createProductResponse = await _erpHttpClient!.PostAsync(createProductUri, createProductContent);
        ((int)createProductResponse.StatusCode).ShouldBe(201);

        await shopUiPage!.GotoAsync(_configuration.ShopUiBaseUrl);
        await shopUiPage.Locator("a[href='/new-order']").ClickAsync();

        await shopUiPage.Locator("[aria-label=\"SKU\"]").FillAsync(sku);
        await shopUiPage.Locator("[aria-label=\"Quantity\"]").FillAsync("5");
        await shopUiPage.Locator("[aria-label=\"Country\"]").FillAsync("US");
        await shopUiPage.Locator("[aria-label=\"Place Order\"]").ClickAsync();

        var successMessageText = await shopUiPage.Locator("[role='alert']").TextContentAsync();
        var match = Regex.Match(successMessageText ?? "", @"Success! Order has been created with Order Number ([\w-]+)");
        match.Success.ShouldBeTrue();
        var orderNumber = match.Groups[1].Value;
        orderNumber.ShouldStartWith("ORD-");

        await shopUiPage.GotoAsync(_configuration.ShopUiBaseUrl);
        await shopUiPage.Locator("a[href='/order-history']").ClickAsync();
        await shopUiPage.Locator("[aria-label='Order Number']").FillAsync(orderNumber);
        await shopUiPage.Locator("[aria-label='Refresh Order List']").ClickAsync();

        var rowSelector = $"xpath=//tr[contains(., '{orderNumber}')]";
        await shopUiPage.Locator(rowSelector).WaitForAsync(new() { State = Microsoft.Playwright.WaitForSelectorState.Visible });
        (await shopUiPage.Locator(rowSelector).IsVisibleAsync()).ShouldBeTrue();

        var viewDetailsSelector = $"xpath=//tr[contains(., '{orderNumber}')]//a[contains(text(), 'View Details')]";
        await shopUiPage.Locator(viewDetailsSelector).ClickAsync();

        (await shopUiPage.Locator("[aria-label='Display Order Number']").TextContentAsync()).ShouldBe(orderNumber);
        (await shopUiPage.Locator("[aria-label='Display SKU']").TextContentAsync()).ShouldBe(sku);
        int.Parse(await shopUiPage.Locator("[aria-label='Display Quantity']").TextContentAsync() ?? "0").ShouldBe(5);

        var unitPriceText = await shopUiPage.Locator("[aria-label='Display Unit Price']").TextContentAsync();
        decimal.Parse((unitPriceText ?? "").Replace("$", "")).ShouldBe(20.00m);

        var totalPriceText = await shopUiPage.Locator("[aria-label='Display Total Price']").TextContentAsync();
        decimal.Parse((totalPriceText ?? "").Replace("$", "")).ShouldBeGreaterThan(0);

        (await shopUiPage.Locator("[aria-label='Display Status']").TextContentAsync()).ShouldBe("PLACED");
    }
}
