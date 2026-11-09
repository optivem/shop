using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenCoupon
{
    IThenCoupon HasDiscountRate(decimal discountRate);

    IThenCoupon IsValidFrom(string validFrom);

    IThenCoupon IsValidTo(string validTo);

    IThenCoupon HasUsageLimit(int usageLimit);

    /// <summary>The coupon has no valid-from time, so it is valid immediately.</summary>
    IThenCoupon IsValidImmediately();

    /// <summary>The coupon has no valid-to time, so it never expires.</summary>
    IThenCoupon NeverExpires();

    /// <summary>The coupon has no usage limit.</summary>
    IThenCoupon HasUnlimitedUsage();

    IThenCoupon HasUsedCount(int expectedUsedCount);

    TaskAwaiter GetAwaiter();
}
