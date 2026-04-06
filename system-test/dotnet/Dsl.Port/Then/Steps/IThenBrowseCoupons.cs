using System.Runtime.CompilerServices;

namespace Dsl.Port.Then.Steps;

public interface IThenBrowseCoupons
{
    IThenBrowseCoupons ContainsCouponWithCode(string expectedCode);

    IThenBrowseCoupons CouponCount(int expectedCount);

    TaskAwaiter GetAwaiter();
}
