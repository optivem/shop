using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseCouponsPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldReturnPublishedCoupon(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCode("BROWSE10").WithDiscountRate(0.1m)
            .When().BrowseCoupons()
            .Then().ShouldSucceed()
            .And().Coupons().ContainsCouponWithCode("BROWSE10");
    }
}
