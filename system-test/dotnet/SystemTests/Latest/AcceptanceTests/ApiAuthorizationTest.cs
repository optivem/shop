using Common;
using Driver.Adapter.Api.Client;
using Driver.Port.Dtos;
using Shouldly;
using SystemTests.Latest.AcceptanceTests.Base;
using Xunit;

namespace SystemTests.Latest.AcceptanceTests;

public class ApiAuthorizationTest : BaseApiClientTest
{
    [SkippableFact]
    public async Task ShouldNotRequireTokenForHealth()
    {
        var result = await ApiClient.As(ApiIdentity.Anonymous).Health().CheckHealthAsync();

        result.ShouldBeSuccess();
    }

    [SkippableFact]
    public async Task ShouldRejectRequestWithoutToken()
    {
        var result = await ApiClient.As(ApiIdentity.Anonymous).Orders().ViewOrderAsync("ORD-NONEXISTENT");

        result.ShouldBeFailure();
        result.Error.Status.ShouldBe(401);
    }

    [SkippableFact]
    public async Task ShouldRejectCustomerBrowsingCoupons()
    {
        var result = await ApiClient.As(ApiIdentity.Customer).Coupons().BrowseCouponsAsync();

        result.ShouldBeFailure();
        result.Error.Status.ShouldBe(403);
    }

    [SkippableFact]
    public async Task ShouldAllowAdminToBrowseCoupons()
    {
        var result = await ApiClient.As(ApiIdentity.Admin).Coupons().BrowseCouponsAsync();

        result.ShouldBeSuccess();
    }

    [SkippableFact]
    public async Task ShouldRejectCustomerPublishingCoupon()
    {
        var result = await ApiClient.As(ApiIdentity.Customer).Coupons().PublishCouponAsync(NewCouponRequest());

        result.ShouldBeFailure();
        result.Error.Status.ShouldBe(403);
    }

    [SkippableFact]
    public async Task ShouldRejectCustomerDeliveringOrder()
    {
        var result = await ApiClient.As(ApiIdentity.Customer).Orders().DeliverOrderAsync("ORD-NONEXISTENT");

        result.ShouldBeFailure();
        result.Error.Status.ShouldBe(403);
    }

    [SkippableFact]
    public async Task ShouldAllowAdminToPublishCoupon()
    {
        var result = await ApiClient.As(ApiIdentity.Admin).Coupons().PublishCouponAsync(NewCouponRequest());

        result.ShouldBeSuccess();
    }

    private static PublishCouponRequest NewCouponRequest() => new()
    {
        Code = "AUTH-" + Guid.NewGuid().ToString("N")[..8].ToUpperInvariant(),
        DiscountRate = "0.10"
    };
}
