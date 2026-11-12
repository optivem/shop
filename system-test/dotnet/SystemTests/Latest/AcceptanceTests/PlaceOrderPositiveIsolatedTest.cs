using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class PlaceOrderPositiveIsolatedTest : BaseAcceptanceTest
{
    private const string CouponListReason = "the UI reads the coupon back from the coupon list, newest first with no paging; coupons published in parallel would push it off the page";

    [Theory]
    [Isolated(CouponListReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-001.5")]
    [Trait("tag", "UC-005.4")]
    public async Task CouponUsageCountHasBeenIncrementedAfterItsBeenUsed(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon().WithCouponCode("SUMMER2025")
            .When().PlaceOrder().WithCouponCode("SUMMER2025")
            .Then().ShouldSucceed()
            .And().Coupon("SUMMER2025")
            .HasUsedCount(1);
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-001.6")]
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
    [Trait("tag", "UC-001.7")]
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
    [Trait("tag", "UC-001.7")]
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
