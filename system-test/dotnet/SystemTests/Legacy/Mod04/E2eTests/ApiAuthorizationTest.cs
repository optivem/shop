using Driver.Adapter.Api.Client;
using Driver.Port.Dtos;
using Shouldly;
using SystemTests.Legacy.Mod04.E2eTests.Base;
using Common;
using Xunit;

namespace SystemTests.Legacy.Mod04.E2eTests;

public class ApiAuthorizationTest : BaseE2eTest
{
    protected override Task SetMyShopClientAsync()
    {
        Skip.If(string.IsNullOrWhiteSpace(_configuration.KeycloakBaseUrl),
            "Authorization tests require KEYCLOAK_URL to be set");
        SetUpMyShopApiClient();
        return Task.CompletedTask;
    }

    [SkippableFact]
    public async Task ShouldNotRequireTokenForHealth()
    {
        var result = await _shopApiClient!.As(ApiIdentity.Anonymous).Health().CheckHealthAsync();

        result.ShouldBeSuccess();
    }

    [SkippableFact]
    public async Task ShouldRejectRequestWithoutToken()
    {
        var result = await _shopApiClient!.As(ApiIdentity.Anonymous).Orders().ViewOrderAsync("ORD-NONEXISTENT");

        result.ShouldBeFailure();
        result.Error.Status.ShouldBe(401);
    }

    [SkippableFact]
    public async Task ShouldRejectCustomerPublishingCoupon()
    {
        var result = await _shopApiClient!.As(ApiIdentity.Customer).Coupons().PublishCouponAsync(NewCouponRequest());

        result.ShouldBeFailure();
        result.Error.Status.ShouldBe(403);
    }

    [SkippableFact]
    public async Task ShouldAllowAdminToPublishCoupon()
    {
        var result = await _shopApiClient!.As(ApiIdentity.Admin).Coupons().PublishCouponAsync(NewCouponRequest());

        result.ShouldBeSuccess();
    }

    private static PublishCouponRequest NewCouponRequest() => new()
    {
        Code = "AUTH-" + Guid.NewGuid().ToString("N")[..8].ToUpperInvariant(),
        DiscountRate = "0.10"
    };
}
