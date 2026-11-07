using Common;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;

namespace Driver.Port.External.Erp;

public interface IErpDriver : IDisposable, IAsyncDisposable
{
    Task<Result<VoidValue, ErpErrorResponse>> GoToErpAsync();

    Task<Result<GetProductResponse, ErpErrorResponse>> GetProductAsync(GetProductRequest request);

    Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductAsync(ReturnsProductRequest request);

    Task<Result<VoidValue, ErpErrorResponse>> ReturnsPromotionAsync(ReturnsPromotionRequest request);

    /// <summary>Makes the ERP unavailable: every ERP API call fails until the driver is disposed.</summary>
    Task<Result<VoidValue, ErpErrorResponse>> ReturnsUnavailableAsync();
}

