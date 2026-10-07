using static SystemTests.Commons.Constants.ErrorMessages;
using SystemTests.Latest.AcceptanceTests.Base;
using SystemTests.Commons.Providers;
using Dsl.Core.UseCase;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PublishCouponNegativeTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("0.0")]
    [ChannelInlineData("-0.01")]
    [ChannelInlineData("-0.15")]
    public async Task CannotPublishCouponWithZeroOrNegativeDiscount(Channel channel, string discountRate)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("INVALID-COUPON")
                .WithDiscountRate(discountRate)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("discountRate", "Discount rate must be greater than 0.00");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("1.01")]
    [ChannelInlineData("2.00")]
    public async Task CannotPublishCouponWithDiscountGreaterThan100Percent(Channel channel, string discountRate)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("INVALID-COUPON")
                .WithDiscountRate(discountRate)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("discountRate", "Discount rate must be at most 1.00");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
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
            .FieldErrorMessage("couponCode", "Coupon code EXISTING-COUPON already exists");
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("0")]
    [ChannelInlineData("-1")]
    [ChannelInlineData("-100")]
    public async Task CannotPublishCouponWithZeroOrNegativeUsageLimit(Channel channel, string usageLimit)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("INVALID-LIMIT")
                .WithDiscountRate(0.15m)
                .WithUsageLimit(usageLimit)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("usageLimit", "Usage limit must be positive");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    [ChannelClassData(typeof(EmptyArgumentsProvider))]
    public async Task ShouldRejectCouponWithBlankCode(Channel channel, string code)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode(code)
                .WithDiscountRate(0.10m)
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("code", "Coupon code must not be blank");
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task CustomerShouldNotBeAbleToPublishCoupon(Channel channel)
    {
        await Scenario(channel)
            .Given().LoggedInAsCustomer()
            .When().PublishCoupon()
            .Then().ShouldFail()
            .ErrorMessage(PermissionDenied);
    }
}
