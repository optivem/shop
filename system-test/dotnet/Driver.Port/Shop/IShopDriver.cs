using Common;
using Driver.Port.Shop.Dtos;
using Driver.Port.Shop.Dtos.Error;

namespace Driver.Port.Shop;

public interface IShopDriver : IAsyncDisposable
{
    Task<Result<VoidValue, SystemError>> GoToShopAsync();
    Task<Result<PlaceOrderResponse, SystemError>> PlaceOrderAsync(PlaceOrderRequest request);
    Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(string? orderNumber);
    Task<Result<VoidValue, SystemError>> PublishCouponAsync(PublishCouponRequest request);
    Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync();
}

