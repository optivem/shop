using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class BrowseCouponsPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    public async Task ShouldBeAbleToBrowseCoupons(Channel channel)
    {
        await Scenario(channel)
            .When().BrowseCoupons()
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-005.4")]
    public async Task ShouldShowHowManyTimesACouponHasBeenUsed(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCouponCode("LIMITED2026").WithUsageLimit(2)
            .And().Order().WithOrderNumber("ORD-1").WithCouponCode("LIMITED2026")
            .And().Order().WithOrderNumber("ORD-2").WithCouponCode("LIMITED2026")
            .When().BrowseCoupons()
            .Then().ShouldSucceed()
            .And().Coupon("LIMITED2026")
            .HasUsedCount(2);
    }
}
