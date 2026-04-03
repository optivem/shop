using Common;
using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;
using Driver.Adapter.External.Erp.Client;
using Driver.Adapter.External.Erp.Client.Dtos.Error;

namespace Driver.Adapter.External.Erp;

public abstract class BaseErpDriver<TClient> : IErpDriver
    where TClient : BaseErpClient
{
    protected readonly TClient _client;
    private bool _disposed;

    protected BaseErpDriver(TClient client)
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

    public virtual Task<Result<VoidValue, ErpErrorResponse>> GoToErpAsync()
        => _client.CheckHealthAsync()
            .MapErrorAsync(error => MapError(error));

    public virtual Task<Result<GetProductResponse, ErpErrorResponse>> GetProductAsync(GetProductRequest request)
        => _client.GetProductAsync(request.Sku)
            .MapAsync(productDetails => new GetProductResponse
            {
                Sku = productDetails.Id,
                Price = productDetails.Price
            })
            .MapErrorAsync(error => MapError(error));

    public abstract Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductAsync(ReturnsProductRequest request);

    protected static ErpErrorResponse MapError(ExtErpErrorResponse extError)
    {
        return new ErpErrorResponse { Message = extError.Message };
    }
}

