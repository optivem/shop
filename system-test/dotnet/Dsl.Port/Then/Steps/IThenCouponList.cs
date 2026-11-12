using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenCouponList
{
    /// <summary>
    /// The list begins with these coupons (aliases), in this order. The coupons table is shared, so coupons published
    /// before the scenario may follow them; a scenario that relies on its coupons being the newest must run isolated.
    /// </summary>
    IThenCouponList StartsWithCoupons(params string[] couponCodes);

    TaskAwaiter GetAwaiter();
}
