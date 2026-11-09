using System.Globalization;
using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Dsl.Port.Then.Steps;
using Optivem.Testing;

namespace SystemTests.Latest.AcceptanceTests;

public class PublishCouponPositiveTest : BaseAcceptanceTest
{
    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-004.2")]
    [Trait("tag", "UC-004.3")]
    public async Task ShouldBeAbleToPublishValidCoupon(Channel channel)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("SUMMER2025")
                .WithDiscountRate(0.15m)
                .WithValidFrom("2024-06-01T00:00:00Z")
                .WithValidTo("2024-08-31T23:59:59Z")
                .WithUsageLimit(100)
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-004.2")]
    [Trait("tag", "UC-004.3")]
    [Trait("tag", "UC-005.2")]
    public async Task ShouldBeAbleToPublishCouponWithEmptyOptionalFields(Channel channel)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("SUMMER2025")
                .WithDiscountRate(0.15m)
                .WithValidFrom(null)
                .WithValidTo(null)
                .WithUsageLimit(null)
            .Then().ShouldSucceed()
            .And().Coupon("SUMMER2025")
            .IsValidImmediately()
            .NeverExpires()
            .HasUnlimitedUsage();
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-004.2")]
    [Trait("tag", "UC-004.3")]
    [Trait("tag", "UC-004.4")]
    [Trait("tag", "UC-005.2")]
    public async Task ShouldBeAbleToCorrectlySaveCoupon(Channel channel)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("SUMMER2025")
                .WithDiscountRate(0.15m)
                .WithValidFrom("2024-06-01T00:00:00Z")
                .WithValidTo("2024-08-31T23:59:00Z")
                .WithUsageLimit(100)
            .Then().ShouldSucceed()
            .And().Coupon("SUMMER2025")
            .HasDiscountRate(0.15m)
            .IsValidFrom("2024-06-01T00:00:00Z")
            .IsValidTo("2024-08-31T23:59:00Z")
            .HasUsageLimit(100)
            .HasUsedCount(0);
    }

    [Theory]
    [ChannelData(ChannelType.API)]
    public async Task ShouldPublishCouponSuccessfully(Channel channel)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("SAVE10")
                .WithDiscountRate(0.10m)
            .Then().ShouldSucceed();
    }

    [Theory]
    [ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]
    [ChannelInlineData("0.01")]
    [ChannelInlineData("0.15")]
    [ChannelInlineData("1.00")]
    [Trait("tag", "UC-004.1")]
    public async Task ShouldAcceptDiscountRateAboveZeroAndUpToOne(Channel channel, string discountRate)
    {
        await Scenario(channel)
            .When().PublishCoupon()
                .WithCouponCode("SAVE")
                .WithDiscountRate(discountRate)
            .Then().ShouldSucceed()
            .And().Coupon("SAVE")
            .HasDiscountRate(decimal.Parse(discountRate, CultureInfo.InvariantCulture));
    }
}
