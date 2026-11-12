using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class PublishCouponNegativeIsolatedTest : BaseAcceptanceTest
{
    private const string CouponListReason = "the UI reads the coupon back from the coupon list, newest first with no paging; coupons published in parallel would push it off the page";

    [Theory]
    [Isolated(CouponListReason)]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-004.9")]
    public async Task CannotPublishCouponWithDuplicateCouponCode(Channel channel)
    {
        await Scenario(channel)
            .Given().Coupon()
                .WithCouponCode("EXISTING-COUPON")
                .WithDiscountRate(0.10m)
            .When().PublishCoupon()
                .WithCouponCode("EXISTING-COUPON")
                .WithDiscountRate(0.20m)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("couponCode", "Coupon code EXISTING-COUPON already exists")
            .And().Coupon("EXISTING-COUPON")
            .HasDiscountRate(0.10m);
    }
}
