using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Driver.Port.Dtos;
using Common.Domain;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class CancelOrderPositiveIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [Isolated("mutates the shared wall clock to year-end times to exercise the cancellation blackout window; parallel runs would clash on the clock")]
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
