using Driver.Adapter.Shared.Client.Http;
using Common;
using Driver.Adapter.External.Tax.Client.Dtos;
using Driver.Adapter.External.Tax.Client.Dtos.Error;

namespace Driver.Adapter.External.Tax.Client;

public abstract class BaseTaxClient : IDisposable
{
    private const string HealthEndpoint = "/health";
    private const string CountriesEndpoint = "/api/countries";

    protected readonly JsonHttpClient<ExtTaxErrorResponse> _httpClient;
    private bool _disposed;

    protected BaseTaxClient(string baseUrl)
    {
        _httpClient = new JsonHttpClient<ExtTaxErrorResponse>(baseUrl);
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
            _httpClient?.Dispose();
        _disposed = true;
    }

    public Task<Result<VoidValue, ExtTaxErrorResponse>> CheckHealthAsync()
        => _httpClient.GetAsync(HealthEndpoint);

    public Task<Result<ExtGetCountryResponse, ExtTaxErrorResponse>> GetCountryAsync(string? country)
        => _httpClient.GetAsync<ExtGetCountryResponse>($"{CountriesEndpoint}/{country}");
}
