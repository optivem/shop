using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Shop.Api.Client.Dtos.Errors;
using Driver.Adapter.Shop.Api.Client;
using Driver.Port.Shop.Dtos;
using Driver.Port.Shop.Dtos.Error;
using Driver.Port.Shop;

namespace Driver.Adapter.Shop.Api;

public class ShopApiDriver : IShopDriver
{
    private readonly ShopApiClient _apiClient;

    public ShopApiDriver(string baseUrl)
    {
        _apiClient = new ShopApiClient(baseUrl);
    }

    public ValueTask DisposeAsync()
    {
        _apiClient?.Dispose();
        return ValueTask.CompletedTask;
    }

    public Task<Result<VoidValue, SystemError>> GoToShopAsync()
        => _apiClient.Health().CheckHealthAsync()
            .MapErrorAsync(MapError);

    private static SystemError MapError(ProblemDetailResponse problemDetail)
    {
        var message = problemDetail.Detail ?? "Request failed";
        if (problemDetail.Errors != null && problemDetail.Errors.Any())
        {
            var fieldErrors = problemDetail.Errors
                .Select(e => new SystemError.FieldError(e.Field ?? "unknown", e.Message ?? string.Empty, e.Code))
                .ToList();
            return SystemError.Of(message, fieldErrors.AsReadOnly());
        }
        return SystemError.Of(message);
    }

    public Task<Result<PlaceOrderResponse, SystemError>> PlaceOrderAsync(PlaceOrderRequest request)
        => _apiClient.Orders().PlaceOrderAsync(request)
            .MapErrorAsync(MapError);

    public Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(string? orderNumber)
        => _apiClient.Orders().ViewOrderAsync(orderNumber)
            .MapErrorAsync(MapError);

    public Task<Result<VoidValue, SystemError>> PublishCouponAsync(PublishCouponRequest request)
        => _apiClient.Coupons().PublishCouponAsync(request)
            .MapErrorAsync(MapError);

    public Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync()
        => _apiClient.Coupons().BrowseCouponsAsync()
            .MapErrorAsync(MapError);
}
