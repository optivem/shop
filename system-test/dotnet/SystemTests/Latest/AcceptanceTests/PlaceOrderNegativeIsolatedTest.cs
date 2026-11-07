using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class PlaceOrderNegativeIsolatedTest : BaseAcceptanceTest
{
    [Theory]
    [Isolated("mutates the shared wall clock to a coupon-expiry time; parallel runs would clash on the clock")]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-001.11")]
    public async Task CannotPlaceOrderWithExpiredCoupon(Channel channel)
    {
        await Scenario(channel)
            .Given().Clock().WithTime("2023-09-01T12:00:00Z")
            .And().Coupon().WithCouponCode("SUMMER2023").WithValidFrom("2023-06-01T00:00:00Z").WithValidTo("2023-08-31T23:59:59Z")
            .When().PlaceOrder().WithCouponCode("SUMMER2023")
            .Then().ShouldFail()
            .ErrorMessage("The request contains one or more validation errors")
            .FieldErrorMessage("couponCode", "Coupon code SUMMER2023 has expired");
    }

    [Theory]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-001.13")]
    public async Task ShouldRejectOrderPlacedAtYearEnd(Channel channel)
    {
        await Scenario(channel)
            .Given().Clock().WithTime("2026-12-31T23:59:30Z")
            .When().PlaceOrder()
            .Then().ShouldFail()
            .ErrorMessage("Orders cannot be placed between 23:59 and 00:00 on December 31st");
    }
}
