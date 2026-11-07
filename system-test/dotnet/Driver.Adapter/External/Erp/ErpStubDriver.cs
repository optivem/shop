using Common;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;
using Driver.Adapter.External.Erp.Client;
using Driver.Adapter.External.Erp.Client.Dtos;
using Driver.Adapter.External.Erp.Client.Dtos.Error;

namespace Driver.Adapter.External.Erp;

/// <summary>
/// ErpStubDriver uses WireMock to stub ERP API responses.
/// This allows tests to run without a real ERP system.
/// </summary>
public class ErpStubDriver : BaseErpDriver<ErpStubClient>
{
    public ErpStubDriver(string baseUrl) : base(new ErpStubClient(baseUrl))
    {
    }

    public override async ValueTask DisposeAsync()
    {
        await _client.RemoveStubsAsync();
        Dispose(true);
        GC.SuppressFinalize(this);
    }

    public override Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductAsync(ReturnsProductRequest request)
    {
        var extProductDetailsResponse = new ExtProductDetailsResponse
        {
            Id = request.Sku!,
            Title = string.Empty,
            Description = string.Empty,
            Price = decimal.Parse(request.Price!),
            Category = string.Empty,
            Brand = string.Empty
        };

        return _client.ConfigureGetProductAsync(extProductDetailsResponse)
            .MapErrorAsync(error => MapError(error));
    }

    public override Task<Result<VoidValue, ErpErrorResponse>> ReturnsPromotionAsync(ReturnsPromotionRequest request)
    {
        var extGetPromotionResponse = new ExtGetPromotionResponse
        {
            PromotionActive = request.PromotionActive,
            Discount = decimal.Parse(request.Discount!)
        };

        return _client.ConfigureGetPromotionAsync(extGetPromotionResponse)
            .MapErrorAsync(error => MapError(error));
    }

    public override Task<Result<VoidValue, ErpErrorResponse>> ReturnsUnavailableAsync()
    {
        return _client.ConfigureUnavailableAsync()
            .MapErrorAsync(error => MapError(error));
    }
}
