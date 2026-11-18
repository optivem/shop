using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Api.Client.Dtos.Errors;
using Driver.Port.Dtos;

namespace Driver.Adapter.Api.Client.Controllers;

public class AdminController
{
    private const string Endpoint = "/api/admin";

    private readonly JsonHttpClient<ProblemDetailResponse> _httpClient;

    public AdminController(JsonHttpClient<ProblemDetailResponse> httpClient)
    {
        _httpClient = httpClient;
    }

    public Task<Result<BrowseCustomersResponse, ProblemDetailResponse>> BrowseCustomersAsync()
        => _httpClient.GetAsync<BrowseCustomersResponse>($"{Endpoint}/customers");
}
