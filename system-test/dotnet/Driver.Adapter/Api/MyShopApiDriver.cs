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
    private TestUser? _requestedUser;
    private bool _anonymous;

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
        _anonymous = identity.Kind == UserIdentityKind.Anonymous;
        _requestedUser = identity.Kind switch
        {
            UserIdentityKind.Default or UserIdentityKind.Anonymous => null,
            UserIdentityKind.Customer => TestUserExtensions.CustomerAt(identity.CustomerIndex),
            UserIdentityKind.Admin => TestUser.Admin,
            _ => throw new ArgumentOutOfRangeException(nameof(identity), identity, null)
        };
    }

    /// <summary>An explicitly requested identity wins; otherwise the operation runs as its default user.</summary>
    private MyShopApiClient RunAs(TestUser operationDefault)
    {
        var user = _requestedUser ?? operationDefault;
        if (_anonymous)
        {
            return _apiClient.As(ApiIdentity.Anonymous);
        }

        return user == TestUser.Admin ? _apiClient.As(ApiIdentity.Admin) : _apiClient.AsCustomer(user);
    }

    public Task<Result<GoToMyShopResponse, SystemError>> GoToMyShopAsync(GoToMyShopRequest request)
        => RunAs(TestUser.Customer).Health().CheckHealthAsync()
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
        => RunAs(TestUser.Customer).Orders().PlaceOrderAsync(request)
            .MapErrorAsync(MapError);

    public Task<Result<CancelOrderResponse, SystemError>> CancelOrderAsync(CancelOrderRequest request)
        => RunAs(TestUser.Customer).Orders().CancelOrderAsync(request.OrderNumber)
            .MapErrorAsync(MapError)
            .MapAsync(_ => new CancelOrderResponse());

    public Task<Result<DeliverOrderResponse, SystemError>> DeliverOrderAsync(DeliverOrderRequest request)
        => RunAs(TestUser.Admin).Orders().DeliverOrderAsync(request.OrderNumber)
            .MapErrorAsync(MapError)
            .MapAsync(_ => new DeliverOrderResponse());

    public Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(ViewOrderRequest request)
        => RunAs(TestUser.Customer).Orders().ViewOrderAsync(request.OrderNumber)
            .MapErrorAsync(MapError);

    public Task<Result<BrowseOrderHistoryResponse, SystemError>> BrowseOrderHistoryAsync(BrowseOrderHistoryRequest request)
        => RunAs(TestUser.Customer).Orders().BrowseOrderHistoryAsync(request.OrderNumber, request.Page, request.Size)
            .MapErrorAsync(MapError);

    public Task<Result<PublishCouponResponse, SystemError>> PublishCouponAsync(PublishCouponRequest request)
        => RunAs(TestUser.Admin).Coupons().PublishCouponAsync(request)
            .MapErrorAsync(MapError)
            .MapAsync(_ => new PublishCouponResponse());

    public Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync(BrowseCouponsRequest request)
        => RunAs(TestUser.Admin).Coupons().BrowseCouponsAsync(request.Page, request.Size)
            .MapErrorAsync(MapError);
}
