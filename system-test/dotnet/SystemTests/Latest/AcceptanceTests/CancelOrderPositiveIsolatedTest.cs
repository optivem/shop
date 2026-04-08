using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.Shop;
using Driver.Port.Shop.Dtos;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class CancelOrderPositiveIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [Time]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("2024-12-31T21:59:59Z")]
    [ChannelInlineData("2024-12-31T22:30:01Z")]
    [ChannelInlineData("2024-12-31T10:00:00Z")]
    [ChannelInlineData("2025-01-01T22:15:00Z")]
    public async Task ShouldBeAbleToCancelOrderOutsideOfBlackoutPeriod31stDecBetween2200And2230(Channel channel, string timeIso)
    {
        await Scenario(channel)
            .Given().Clock().WithTime(timeIso)
            .And().Order().WithStatus(OrderStatus.Placed)
            .When().CancelOrder()
            .Then().ShouldSucceed();
    }
}
