using Common;
using Driver.Port.External.Erp.Dtos;
using Driver.Port.External.Erp.Dtos.Error;
using Driver.Adapter.External.Erp.Client;
using Driver.Adapter.External.Erp.Client.Dtos;
using Driver.Adapter.External.Erp.Client.Dtos.Error;

namespace Driver.Adapter.External.Erp;

public class ErpRealDriver : BaseErpDriver<ErpRealClient>
{
    public ErpRealDriver(string baseUrl) : base(new ErpRealClient(baseUrl))
    {
    }

    public override Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductAsync(ReturnsProductRequest request)
    {
        var createProductRequest = new ExtCreateProductRequest
        {
            Id = request.Sku,
            Title = request.Title ?? DefaultTitle,
            Description = request.Description ?? DefaultDescription,
            Category = request.Category ?? DefaultCategory,
            Brand = request.Brand ?? DefaultBrand,
            Price = request.Price
        };

        return _client.CreateProductAsync(createProductRequest)
            .MapErrorAsync(error => MapError(error));
    }

    /// <summary>The real ERP lists every product it holds, so staging a list means creating each product in it.</summary>
    public override async Task<Result<VoidValue, ErpErrorResponse>> ReturnsProductsAsync(ReturnsProductsRequest request)
    {
        foreach (var product in request.Products)
        {
            var result = await ReturnsProductAsync(product);
            if (result.IsFailure)
            {
                return result;
            }
        }

        return Result.Success<ErpErrorResponse>();
    }

    public override Task<Result<VoidValue, ErpErrorResponse>> ReturnsPromotionAsync(ReturnsPromotionRequest request)
        => Task.FromResult(Result.Success<ErpErrorResponse>());

    /// <summary>
    /// The real ERP cannot be taken down on demand, so this throws rather than silently succeeding,
    /// which would let an ERP-unavailable test pass for the wrong reason.
    /// </summary>
    public override Task<Result<VoidValue, ErpErrorResponse>> ReturnsUnavailableAsync()
        => throw new NotSupportedException(
            "ErpRealDriver cannot make the ERP unavailable; run this scenario against the ERP stub");
}
