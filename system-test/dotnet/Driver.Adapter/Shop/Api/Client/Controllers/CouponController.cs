using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Shop.Api.Client.Dtos.Errors;
using Driver.Port.Shop.Dtos;

namespace Driver.Adapter.Shop.Api.Client.Controllers;

public class CouponController
{
    private const string Endpoint = "/api/coupons";

    private readonly JsonHttpClient<ProblemDetailResponse> _httpClient;

    public CouponController(JsonHttpClient<ProblemDetailResponse> httpClient)
    {
        _httpClient = httpClient;
    }

    public Task<Result<VoidValue, ProblemDetailResponse>> PublishCouponAsync(PublishCouponRequest request)
        => _httpClient.PostAsync(Endpoint, request);

    public Task<Result<BrowseCouponsResponse, ProblemDetailResponse>> BrowseCouponsAsync()
        => _httpClient.GetAsync<BrowseCouponsResponse>(Endpoint);
}
