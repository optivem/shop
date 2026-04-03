using Driver.Adapter.External.Clock.Client.Dtos;

using Driver.Adapter.External.Clock.Client.Dtos.Error;

using Driver.Adapter.Shared.Client.Http;

using Common;

using Driver.Adapter.Shared.Client.WireMock;



namespace Driver.Adapter.External.Clock.Client;



public class ClockStubClient : IDisposable

{

    private const string HealthEndpoint = "/health";

    private const string TimeEndpoint = "/api/time";

    private const string ClockTimeEndpoint = "/clock/api/time";



    private readonly JsonHttpClient<ExtClockErrorResponse> _httpClient;

    private readonly JsonWireMockClient _wireMockClient;

    private bool _disposed;



    public ClockStubClient(string baseUrl)

    {

        _httpClient = new JsonHttpClient<ExtClockErrorResponse>(baseUrl);

        _wireMockClient = new JsonWireMockClient(baseUrl);

    }



    public void Dispose()

    {

        Dispose(true);

        GC.SuppressFinalize(this);

    }



    protected virtual void Dispose(bool disposing)

    {

        if (_disposed) return;

        if (disposing)

        {

            _httpClient.Dispose();

            _wireMockClient.Dispose();

        }

        _disposed = true;

    }



    public Task<Result<VoidValue, ExtClockErrorResponse>> CheckHealthAsync()

        => _httpClient.GetAsync(HealthEndpoint);



    public Task<Result<ExtGetTimeResponse, ExtClockErrorResponse>> GetTimeAsync()

        => _httpClient.GetAsync<ExtGetTimeResponse>(TimeEndpoint);



    public Task<Result<VoidValue, ExtClockErrorResponse>> ConfigureGetTimeAsync(ExtGetTimeResponse response)

        => _wireMockClient.StubGetAsync(ClockTimeEndpoint, HttpStatus.Ok, response)

            .MapErrorAsync(ExtClockErrorResponse.From);



    public Task RemoveStubsAsync() => _wireMockClient.RemoveStubsAsync();

}





