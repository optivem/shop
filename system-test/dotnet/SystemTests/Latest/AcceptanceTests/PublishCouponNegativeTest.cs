using SystemTests.Latest.AcceptanceTests.Base;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PublishCouponNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API)]
    [ChannelInlineData("")]
    [ChannelInlineData(" ")]
    [ChannelInlineData(null)]
    public async Task ShouldRejectCouponWithBlankCode(Channel channel, string? code)
    {
        await Scenario(channel)
            .When().PublishCoupon().WithCode(code).WithDiscountRate(0.1m)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("code", "Coupon code must not be blank");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [ChannelInlineData(0.0)]
    [ChannelInlineData(-0.1)]
    public async Task ShouldRejectCouponWithNonPositiveDiscountRate(Channel channel, double discountRate)
    {
        await Scenario(channel)
            .When().PublishCoupon().WithCode("SAVE10").WithDiscountRate((decimal)discountRate)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("discountRate", "Discount rate must be greater than 0.00");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [ChannelInlineData(1.01)]
    [ChannelInlineData(2.0)]
    public async Task ShouldRejectCouponWithDiscountRateAboveOne(Channel channel, double discountRate)
    {
        await Scenario(channel)
            .When().PublishCoupon().WithCode("SAVE10").WithDiscountRate((decimal)discountRate)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("discountRate", "Discount rate must be at most 1.00");
    }
}
