using Dsl.Core.Shared;
using Shouldly;
using Driver.Port.Dtos;
using Common;

namespace Dsl.Core.UseCase.UseCases;

public class BrowseCouponsVerification : ResponseVerification<BrowseCouponsResponse>
{
    public BrowseCouponsVerification(BrowseCouponsResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public BrowseCouponsVerification HasCouponWithCode(string couponCodeAlias)
    {
        FindCouponByCode(couponCodeAlias);
        return this;
    }

    public BrowseCouponsVerification CouponHasDiscountRate(string couponCodeAlias, decimal expectedDiscountRate)
    {
        var coupon = FindCouponByCode(couponCodeAlias);
        coupon.DiscountRate.ShouldBe(expectedDiscountRate, $"Expected coupon '{couponCodeAlias}' to have discount rate {expectedDiscountRate:F2}");
        return this;
    }

    public BrowseCouponsVerification CouponHasValidFrom(string couponCodeAlias, string? expectedValidFrom)
    {
        var coupon = FindCouponByCode(couponCodeAlias);
        DateTime? expectedValidFromDateTime = Converter.ToDateTime(expectedValidFrom);
        coupon.ValidFrom.ShouldBe(expectedValidFromDateTime, $"Expected coupon '{couponCodeAlias}' to have validFrom '{expectedValidFrom}'");
        return this;
    }

    public BrowseCouponsVerification CouponHasValidTo(string couponCodeAlias, string? expectedValidTo)
    {
        var coupon = FindCouponByCode(couponCodeAlias);
        DateTime? expectedValidToDateTime = Converter.ToDateTime(expectedValidTo);
        coupon.ValidTo.ShouldBe(expectedValidToDateTime, $"Expected coupon '{couponCodeAlias}' to have validTo '{expectedValidTo}'");
        return this;
    }

    public BrowseCouponsVerification CouponHasUsageLimit(string couponCodeAlias, int expectedUsageLimit)
    {
        var coupon = FindCouponByCode(couponCodeAlias);
        coupon.UsageLimit.ShouldBe(expectedUsageLimit, $"Expected coupon '{couponCodeAlias}' to have usage limit {expectedUsageLimit}");
        return this;
    }

    public BrowseCouponsVerification CouponHasNoValidFrom(string couponCodeAlias)
    {
        var coupon = FindCouponByCode(couponCodeAlias);
        coupon.ValidFrom.ShouldBeNull($"Expected coupon '{couponCodeAlias}' to have no validFrom (valid immediately)");
        return this;
    }

    public BrowseCouponsVerification CouponHasNoValidTo(string couponCodeAlias)
    {
        var coupon = FindCouponByCode(couponCodeAlias);
        coupon.ValidTo.ShouldBeNull($"Expected coupon '{couponCodeAlias}' to have no validTo (never expires)");
        return this;
    }

    public BrowseCouponsVerification CouponHasNoUsageLimit(string couponCodeAlias)
    {
        var coupon = FindCouponByCode(couponCodeAlias);
        // The system stores "no usage limit" as int.MaxValue, which the UI shows as "Unlimited".
        (coupon.UsageLimit is null or int.MaxValue).ShouldBeTrue($"Expected coupon '{couponCodeAlias}' to have no usage limit (unlimited), but was {coupon.UsageLimit}");
        return this;
    }

    public BrowseCouponsVerification CouponHasUsedCount(string couponCode, int expectedUsedCount)
    {
        var coupon = FindCouponByCode(couponCode);
        coupon.UsedCount.ShouldBe(expectedUsedCount, $"Expected coupon '{couponCode}' to have used count {expectedUsedCount}");
        return this;
    }

    private CouponDto FindCouponByCode(string couponCodeAlias)
    {
        Response.ShouldNotBeNull();
        Response.Coupons.ShouldNotBeNull("No coupons found in response");

        var couponCode = Context.GetParamValue(couponCodeAlias);

        var coupon = Response.Coupons.FirstOrDefault(c => string.Equals(couponCode, c.Code));

        coupon.ShouldNotBeNull($"Coupon with code '{couponCode}' not found. Available coupons: [{string.Join(", ", Response.Coupons.Select(c => c.Code))}]");

        return coupon;
    }
}
