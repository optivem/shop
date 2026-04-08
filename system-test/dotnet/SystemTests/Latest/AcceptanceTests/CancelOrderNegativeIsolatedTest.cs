using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Driver.Port.Shop.Dtos;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class CancelOrderNegativeIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [Time]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("2024-12-31T22:00:00Z")]
    [ChannelInlineData("2026-12-31T22:00:01Z")]
    [ChannelInlineData("2025-12-31T22:15:00Z")]
    [ChannelInlineData("2028-12-31T22:29:59Z")]
    [ChannelInlineData("2021-12-31T22:30:00Z")]
    public async Task CannotCancelAnOrderOn31stDecBetween2200And2230(Channel channel, string timeIso)
    {
        await Scenario(channel)
            .Given().Clock().WithTime(timeIso)
            .And().Order().WithStatus(OrderStatus.Placed)
            .When().CancelOrder()
            .Then().ShouldFail()
            .ErrorMessage("Order cancellation is not allowed on December 31st between 22:00 and 23:00")
            .And().Order()
            .HasStatus(OrderStatus.Placed);
    }
}
