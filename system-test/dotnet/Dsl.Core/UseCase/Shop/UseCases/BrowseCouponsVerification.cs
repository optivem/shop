using Dsl.Core.Shared;
using Driver.Port.Shop.Dtos;
using Shouldly;

namespace Dsl.Core.Shop.UseCases;

public class BrowseCouponsVerification : ResponseVerification<BrowseCouponsResponse>
{
    public BrowseCouponsVerification(BrowseCouponsResponse response, UseCaseContext context)
        : base(response, context)
    {
    }

    public BrowseCouponsVerification ContainsCouponWithCode(string expectedCode)
    {
        Response.Coupons.ShouldContain(
            c => c.Code == expectedCode,
            $"Expected coupon with code '{expectedCode}' to be present, but was not found");
        return this;
    }

    public BrowseCouponsVerification CouponCount(int expectedCount)
    {
        Response.Coupons.Count.ShouldBe(expectedCount,
            $"Expected {expectedCount} coupons, but found {Response.Coupons.Count}");
        return this;
    }
}
