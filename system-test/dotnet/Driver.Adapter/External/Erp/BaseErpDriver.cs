using Common;
using Driver.Port.External.Erp;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;
using Driver.Adapter.External.Erp.Client;
using Driver.Adapter.External.Erp.Client.Dtos;
using Driver.Adapter.External.Erp.Client.Dtos.Error;

namespace Driver.Adapter.External.Erp;

public abstract class BaseErpDriver<TClient> : IErpDriver
    where TClient : BaseErpClient
{
    // Product details staged when a scenario does not specify them; shared so the stub and real ERP stay in parity.
    protected const string DefaultTitle = "Test Product Title";
    protected const string DefaultDescription = "Test Product Description";
    protected const string DefaultCategory = "Test Category";
    protected const string DefaultBrand = "Test Brand";

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
            .MapAsync(ToGetProductResponse)
            .MapErrorAsync(error => MapError(error));

    public virtual Task<Result<GetProductsResponse, ErpErrorResponse>> GetProductsAsync()
        => _client.GetProductsAsync()
            .MapAsync(products => new GetProductsResponse
            {
                Products = products.Select(ToGetProductResponse).ToList()
            })
            .MapErrorAsync(error => MapError(error));

    public abstract Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductAsync(ReturnsProductRequest request);

    public abstract Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductsAsync(ReturnsProductsRequest request);

    public abstract Task<Result<VoidValue, ErpErrorResponse>> ReturnsPromotionAsync(ReturnsPromotionRequest request);

    public abstract Task<Result<VoidValue, ErpErrorResponse>> ReturnsUnavailableAsync();

    public abstract Task<Result<VoidValue, ErpErrorResponse>> ReturnsAvailableAsync();

    private static GetProductResponse ToGetProductResponse(ExtProductDetailsResponse productDetails)
        => new()
        {
            Sku = productDetails.Id,
            Title = productDetails.Title,
            Description = productDetails.Description,
            Category = productDetails.Category,
            Brand = productDetails.Brand,
            Price = productDetails.Price
        };

    protected static ErpErrorResponse MapError(ExtErpErrorResponse extError)
    {
        return new ErpErrorResponse { Message = extError.Message };
    }
}

