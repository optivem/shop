using SystemTests.Latest.AcceptanceTests.Base;
using Dsl.Core.UseCase;
using Optivem.Testing;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

[Collection("Isolated")]
[Trait("Category", "isolated")]
public class PlaceOrderNegativeIsolatedTest : BaseAcceptanceTest
{
    private const string ErpUnavailable = "Product information can't be retrieved right now";
    // A down ERP answers at once, so only the retry backoff (well under a second) delays the 503.
    private const int ErpDownFailFastSeconds = 5;
    // A slow ERP costs up to 3 attempts x 2s timeout plus backoff; the stubbed ERP would take 20s.
    private const int ErpSlowFailFastSeconds = 12;

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

    [Theory]
    [Isolated("takes the shared ERP stub down; parallel runs would see every ERP call fail")]
    [ChannelData(ChannelType.UI, ChannelType.API)]
    [Trait("tag", "UC-001.14")]
    public async Task ShouldRejectOrderWhenErpIsUnavailable(Channel channel)
    {
        await Scenario(channel)
            .Given().Erp().Unavailable()
            .When().PlaceOrder()
            .Then().ShouldFail()
            .ErrorMessage(ErpUnavailable);
    }

    // Resilience: the system fails fast with 503 + Retry-After instead of hanging or guessing a price.
    // API only, because the assertions are about the HTTP response.
    [Theory]
    [Isolated("takes the shared ERP stub down; parallel runs would see every ERP call fail")]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-001.14")]
    public async Task ShouldFailFastWithServiceUnavailableWhenErpIsDown(Channel channel)
    {
        await Scenario(channel)
            .Given().Erp().Unavailable()
            .When().PlaceOrder()
            .Then().ShouldFail()
            .ErrorMessage(ErpUnavailable)
            .ServiceUnavailable()
            .FailedWithinSeconds(ErpDownFailFastSeconds);
    }

    // The slow ERP answers only after 20s, far past the 2s outbound timeout: the call must give up on its
    // own (timeout times retries) well before the ERP would have answered.
    [Theory]
    [Isolated("makes the shared ERP stub slow; parallel runs would see every ERP call time out")]
    [ChannelData(ChannelType.API)]
    [Trait("tag", "UC-001.14")]
    public async Task ShouldFailFastWithServiceUnavailableWhenErpIsSlow(Channel channel)
    {
        await Scenario(channel)
            .Given().Erp().Slow()
            .When().PlaceOrder()
            .Then().ShouldFail()
            .ErrorMessage(ErpUnavailable)
            .ServiceUnavailable()
            .FailedWithinSeconds(ErpSlowFailFastSeconds);
    }
}
