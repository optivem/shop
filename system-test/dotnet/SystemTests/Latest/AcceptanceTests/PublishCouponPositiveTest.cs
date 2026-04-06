using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PublishCouponPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldPublishCouponSuccessfully(Channel channel)
    {
        await Scenario(channel)
            .When().PublishCoupon().WithCode("SAVE10").WithDiscountRate(0.1m)
            .Then().ShouldSucceed();
    }
}
