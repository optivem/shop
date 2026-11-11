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
        return _client.ConfigureGetProductAsync(ToExtProductDetailsResponse(request))
            .MapErrorAsync(error => MapError(error));
    }

    public override async Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductsAsync(ReturnsProductsRequest request)
    {
        var extProducts = request.Products.Select(ToExtProductDetailsResponse).ToList();

        // The ERP serves each listed product on its own as well (GET /api/products/{sku}), as the real ERP does.
        foreach (var extProduct in extProducts)
        {
            var result = await _client.ConfigureGetProductAsync(extProduct);
            if (result.IsFailure)
            {
                return result.MapError(error => MapError(error));
            }
        }

        return await _client.ConfigureGetProductsAsync(extProducts)
            .MapErrorAsync(error => MapError(error));
    }

    private static ExtProductDetailsResponse ToExtProductDetailsResponse(ReturnsProductRequest request)
        => new()
        {
            Id = request.Sku!,
            Title = request.Title ?? DefaultTitle,
            Description = request.Description ?? DefaultDescription,
            Price = decimal.Parse(request.Price!),
            Category = request.Category ?? DefaultCategory,
            Brand = request.Brand ?? DefaultBrand
        };

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

    public override Task<Result<VoidValue, ErpErrorResponse>> ReturnsAvailableAsync()
    {
        return _client.RemoveUnavailableAsync()
            .MapErrorAsync(error => MapError(error));
    }
}
