using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Api.Client.Dtos.Errors;
using Driver.Adapter.Api.Client;
using Driver.Port.Dtos;
using Driver.Port.Dtos.Error;
using Driver.Port;

namespace Driver.Adapter.Api;

public class MyShopApiDriver : IMyShopDriver
{
    private readonly MyShopApiClient _apiClient;

    public MyShopApiDriver(string baseUrl, string? keycloakBaseUrl = null)
    {
        _apiClient = new MyShopApiClient(baseUrl, keycloakBaseUrl);
    }

    public ValueTask DisposeAsync()
    {
        _apiClient?.Dispose();
        GC.SuppressFinalize(this);
        return ValueTask.CompletedTask;
    }

    public void ActAs(UserIdentity identity)
    {
        switch (identity.Kind)
        {
            case UserIdentityKind.Default:
                _apiClient.As(ApiIdentity.Default);
                break;
            case UserIdentityKind.Anonymous:
                _apiClient.As(ApiIdentity.Anonymous);
                break;
            case UserIdentityKind.Customer:
                _apiClient.AsCustomer(TestUserExtensions.CustomerAt(identity.CustomerIndex));
                break;
            case UserIdentityKind.Admin:
                _apiClient.As(ApiIdentity.Admin);
                break;
            default:
                throw new ArgumentOutOfRangeException(nameof(identity), identity, null);
        }
    }

    public Task<Result<GoToMyShopResponse, SystemError>> GoToMyShopAsync(GoToMyShopRequest request)
        => _apiClient.Health().CheckHealthAsync()
            .MapErrorAsync(MapError)
            .MapAsync(_ => new GoToMyShopResponse());

    private static SystemError MapError(ProblemDetailResponse problemDetail)
    {
        var message = problemDetail.Detail ?? "Request failed";
        if (problemDetail.Errors != null && problemDetail.Errors.Count > 0)
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

    public Task<Result<CancelOrderResponse, SystemError>> CancelOrderAsync(CancelOrderRequest request)
        => _apiClient.Orders().CancelOrderAsync(request.OrderNumber)
            .MapErrorAsync(MapError)
            .MapAsync(_ => new CancelOrderResponse());

    public Task<Result<DeliverOrderResponse, SystemError>> DeliverOrderAsync(DeliverOrderRequest request)
        => _apiClient.Orders().DeliverOrderAsync(request.OrderNumber)
            .MapErrorAsync(MapError)
            .MapAsync(_ => new DeliverOrderResponse());

    public Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(ViewOrderRequest request)
        => _apiClient.Orders().ViewOrderAsync(request.OrderNumber)
            .MapErrorAsync(MapError);

    public Task<Result<BrowseOrderHistoryResponse, SystemError>> BrowseOrderHistoryAsync(BrowseOrderHistoryRequest request)
        => _apiClient.Orders().BrowseOrderHistoryAsync(request.OrderNumber)
            .MapErrorAsync(MapError);

    public Task<Result<PublishCouponResponse, SystemError>> PublishCouponAsync(PublishCouponRequest request)
        => _apiClient.Coupons().PublishCouponAsync(request)
            .MapErrorAsync(MapError)
            .MapAsync(_ => new PublishCouponResponse());

    public Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync(BrowseCouponsRequest request)
        => _apiClient.Coupons().BrowseCouponsAsync()
            .MapErrorAsync(MapError);
}
