using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Api.Client.Dtos.Errors;
using Driver.Port.Dtos;

namespace Driver.Adapter.Api.Client.Controllers;

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

    public Task<Result<BrowseCouponsResponse, ProblemDetailResponse>> BrowseCouponsAsync(string? page, string? size)
        => _httpClient.GetAsync<BrowseCouponsResponse>(Endpoint + QueryString.Of(("page", page), ("size", size)));
}
