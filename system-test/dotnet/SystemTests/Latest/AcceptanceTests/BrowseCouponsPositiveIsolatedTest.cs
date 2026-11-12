using Dsl.Core.UseCase;
using Dsl.Port.Given;
using Optivem.Testing;
using SystemTests.Latest.AcceptanceTests.Base;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

/// <summary>
/// The coupon list, newest first. The coupons table is shared and never emptied, so these tests check that the
/// scenario's coupons come first, in order: true only while no other test publishes a coupon. Not covered: the total
/// of 3 and the empty page 3 of UC-005.3, and UC-005.7 (no coupon published), which need a coupon set of their own.
/// </summary>
[Collection("Isolated")]
[Trait("Category", "isolated")]
public class BrowseCouponsPositiveIsolatedTest : BaseAcceptanceTest
{
    private const string NewestReason = "checks that its coupons are the newest; a coupon published in parallel would come before them";

    private IGivenStage GivenThreeCouponsPublishedInOrder(Channel channel)
        => Scenario(channel)
            .Given().Coupon().WithCouponCode("SUMMER2025").WithDiscountRate("0.15")
                .WithValidFrom("2025-06-01T00:00:00Z").WithValidTo("2025-08-31T23:59:59Z").WithUsageLimit(100)
            .And().Coupon().WithCouponCode("SAVE10").WithDiscountRate("0.10")
            .And().Coupon().WithCouponCode("LIMITED2026").WithDiscountRate("0.20")
                .WithValidFrom("2026-01-01T00:00:00Z").WithUsageLimit(2)
            .And().LoggedInAsAdmin();

    [Theory]
    [Isolated(NewestReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-005.1")]
    public async Task ShouldListEveryCouponNewestFirst(Channel channel)
    {
        await GivenThreeCouponsPublishedInOrder(channel)
            .When().BrowseCoupons()
            .Then().ShouldSucceed()
            .CouponList().StartsWithCoupons("LIMITED2026", "SAVE10", "SUMMER2025");
    }

    // Page 2 also holds the coupons published before the scenario, after SUMMER2025.
    [Theory]
    [Isolated(NewestReason)]
    [ChannelData(ChannelType.API)]
    [ChannelInlineData("1", "LIMITED2026,SAVE10")]
    [ChannelInlineData("2", "SUMMER2025")]
    [Trait("tag", "UC-005.3")]
    public async Task ShouldReadTheCouponsOnePageAtATime(Channel channel, string page, string coupons)
    {
        await GivenThreeCouponsPublishedInOrder(channel)
            .When().BrowseCoupons().WithPage(page).WithSize("2")
            .Then().ShouldSucceed()
            .CouponList().StartsWithCoupons(coupons.Split(','));
    }
}
