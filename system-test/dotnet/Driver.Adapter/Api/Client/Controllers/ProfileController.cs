using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Api.Client.Dtos.Errors;
using Driver.Port.Dtos;

namespace Driver.Adapter.Api.Client.Controllers;

public class ProfileController
{
    private const string Endpoint = "/api/account/profile";

    private readonly JsonHttpClient<ProblemDetailResponse> _httpClient;

    public ProfileController(JsonHttpClient<ProblemDetailResponse> httpClient)
    {
        _httpClient = httpClient;
    }

    public Task<Result<ViewProfileResponse, ProblemDetailResponse>> ViewProfileAsync()
        => _httpClient.GetAsync<ViewProfileResponse>(Endpoint);

    public Task<Result<ViewProfileResponse, ProblemDetailResponse>> UpdateProfileAsync(UpdateProfileRequest request)
        => _httpClient.PutAsync<ViewProfileResponse>(Endpoint, request);
}
