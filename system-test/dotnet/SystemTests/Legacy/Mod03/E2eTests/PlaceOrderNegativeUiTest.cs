using SystemTests.Commons.Constants;
using SystemTests.Legacy.Mod03.E2eTests.Base;
using Shouldly;
using Xunit;

namespace SystemTests.Legacy.Mod03.E2eTests;

public class PlaceOrderNegativeUiTest : BaseE2eTest
{
    protected override async Task SetShopRawAsync()
    {
        await SetUpShopBrowserAsync();
    }

    [Fact]
    public async Task ShouldRejectOrderWithNonIntegerQuantity()
    {
        await shopUiPage!.GotoAsync(_configuration.ShopUiBaseUrl);
        await shopUiPage.Locator("a[href='/new-order']").ClickAsync();

        await shopUiPage.Locator("[aria-label=\"SKU\"]").FillAsync(CreateUniqueSku(Defaults.SKU));
        await shopUiPage.Locator("[aria-label=\"Quantity\"]").FillAsync("invalid-quantity");
        await shopUiPage.Locator("[aria-label=\"Place Order\"]").ClickAsync();

        var errorAlert = shopUiPage.Locator("[role='alert'][data-notification-id]");
        await errorAlert.WaitForAsync();
        (await errorAlert.IsVisibleAsync()).ShouldBeTrue();
        var errorText = await errorAlert.TextContentAsync();
        (errorText ?? "").ShouldContain("The request contains one or more validation errors");
        (errorText ?? "").ShouldContain("quantity");
        (errorText ?? "").ShouldContain("Quantity must be an integer");
    }
}
