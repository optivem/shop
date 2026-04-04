using SystemTests.Legacy.Mod10.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Legacy.Mod10.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class PlaceOrderPositiveIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldApplyFullPriceOnWeekday(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithUnitPrice(20.00m)
            .And().Clock().WithWeekday()
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
            .And().Promotion().WithActive(true).WithDiscount("0.5")
            .When().PlaceOrder().WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
                .HasTotalPrice(50.00m);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldRecordPlacementTimestamp(Channel channel)
    {
        (await Scenario(channel)
            .Given().Clock()
                .WithTime("2026-01-15T10:30:00Z")
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Clock())
            .HasTime("2026-01-15T10:30:00Z");
    }
}
