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
        (await Scenario(channel)
            .Given().Clock().WithTime("2026-01-15T10:30:00Z")
            .When().PlaceOrder()
            .Then().ShouldSucceed()
            .And().Clock())
            .HasTime("2026-01-15T10:30:00Z");
    }

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
    public async Task ShouldApplyWeekendDiscount(Channel channel)
    {
        await Scenario(channel)
            .Given().Product().WithUnitPrice(20.00m)
            .And().Clock().WithWeekend()
            .When().PlaceOrder().WithQuantity(5)
            .Then().ShouldSucceed()
            .And().Order()
            .HasTotalPrice(50.00m);
    }
}
