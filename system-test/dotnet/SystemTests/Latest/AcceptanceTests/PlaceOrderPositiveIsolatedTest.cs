using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class PlaceOrderPositiveIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRecordPlacementTimestamp(Channel channel)
    {
        await Scenario(channel)
            .Given().Clock().WithTime("2026-01-15T10:30:00Z")
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Clock()
            .HasTime("2026-01-15T10:30:00Z");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldApplyFullPriceWithoutPromotion(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithUnitPrice(20.00m)
            .And().Promotion().WithActive(false)
            .And().Country().WithTaxRate(0.00m)
            .When().PlaceOrder().WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
            .HasTotalPrice(100.00m);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldApplyDiscountWhenPromotionIsActive(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithUnitPrice(20.00m)
            .And().Promotion().WithActive(true).WithDiscount(0.5m)
            .And().Country().WithTaxRate(0.00m)
            .When().PlaceOrder().WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
            .HasTotalPrice(50.00m);
    }
}
