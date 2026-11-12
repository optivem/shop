using Common;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;

namespace Driver.Port.External.Erp;

public interface IErpDriver : IDisposable, IAsyncDisposable
{
    Task<Result<VoidValue, ErpErrorResponse>> GoToErpAsync();

    Task<Result<GetProductResponse, ErpErrorResponse>> GetProductAsync(GetProductRequest request);

    Task<Result<GetProductsResponse, ErpErrorResponse>> GetProductsAsync();

    Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductAsync(ReturnsProductRequest request);

    Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductsAsync(ReturnsProductsRequest request);

    Task<Result<VoidValue, ErpErrorResponse>> ReturnsPromotionAsync(ReturnsPromotionRequest request);

    /// <summary>Makes the ERP unavailable: every ERP API call fails until the driver is disposed.</summary>
    Task<Result<VoidValue, ErpErrorResponse>> ReturnsUnavailableAsync();

    /// <summary>Makes the ERP slow: every ERP API call answers only after a delay well past the system's outbound timeout, until the driver is disposed.</summary>
    Task<Result<VoidValue, ErpErrorResponse>> ReturnsSlowAsync();

    /// <summary>Ends an outage started by <see cref="ReturnsUnavailableAsync"/>: the ERP answers with what is staged again.</summary>
    Task<Result<VoidValue, ErpErrorResponse>> ReturnsAvailableAsync();

    /// <summary>The ERP no longer offers this product: asking for it answers "not found", whatever was staged before.</summary>
    Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductNotFoundAsync(GetProductRequest request);
}

