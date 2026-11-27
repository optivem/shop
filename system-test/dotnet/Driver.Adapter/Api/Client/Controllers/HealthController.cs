using Common;

using Driver.Adapter.Shared.Client.Http;

using Driver.Adapter.Api.Client.Dtos.Errors;



namespace Driver.Adapter.Api.Client.Controllers;



public class HealthController

{

    private const string Endpoint = "/health";

    /// <summary>Readiness: answers 200 only when the system can serve requests, its database included.</summary>

    private const string ReadinessEndpoint = "/readyz";



    private readonly JsonHttpClient<ProblemDetailResponse> _httpClient;



    public HealthController(JsonHttpClient<ProblemDetailResponse> httpClient)

    {

        _httpClient = httpClient;

    }



    public Task<Result<VoidValue, ProblemDetailResponse>> CheckHealthAsync()

        => _httpClient.GetAsync(Endpoint);



    public Task<Result<VoidValue, ProblemDetailResponse>> CheckReadinessAsync()

        => _httpClient.GetAsync(ReadinessEndpoint);

}





