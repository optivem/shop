using Common;
using Driver.Adapter.External.Tax.Client;
using Driver.Port.External.Tax;
using Driver.Port.External.Tax.Dtos;
using Driver.Port.External.Tax.Dtos.Error;
using Driver.Adapter.External.Tax.Client.Dtos.Error;

namespace Driver.Adapter.External.Tax;

public abstract class BaseTaxDriver<TClient> : ITaxDriver where TClient : BaseTaxClient
{
    protected readonly TClient _client;
    private bool _disposed;

    protected BaseTaxDriver(TClient client)
    {
        _client = client;
    }

    public void Dispose()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    public virtual ValueTask DisposeAsync()
    {
        Dispose(true);
        GC.SuppressFinalize(this);
        return ValueTask.CompletedTask;
    }

    protected virtual void Dispose(bool disposing)
    {
        if (_disposed) return;
        if (disposing)
            _client?.Dispose();
        _disposed = true;
    }

    public virtual Task<Result<VoidValue, TaxErrorResponse>> GoToTaxAsync()
        => _client.CheckHealthAsync()
            .MapErrorAsync(MapError);

    public virtual Task<Result<GetTaxResponse, TaxErrorResponse>> GetTaxRateAsync(string? country)
        => _client.GetCountryAsync(country)
            .MapAsync(taxRateResponse => new GetTaxResponse
            {
                Country = taxRateResponse.Id,
                TaxRate = taxRateResponse.TaxRate
            })
            .MapErrorAsync(MapError);

    public abstract Task<Result<VoidValue, TaxErrorResponse>> ReturnsTaxRateAsync(ReturnsTaxRateRequest request);

    protected static TaxErrorResponse MapError(ExtTaxErrorResponse errorResponse)
        => new TaxErrorResponse { Message = errorResponse.Message };
}
