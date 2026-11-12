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
            UserIdentityKind.Customer => TestUser.ForCustomer(identity),
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

    private static SystemError MapError(ProblemDetailResponse problemDetail) => SystemErrorMapper.From(problemDetail);

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
    {
        RequireNotSet("followUp", request.FollowUp);
        return RunAs(TestUser.Customer).Orders().BrowseOrderHistoryAsync(request.OrderNumber, request.Page, request.Size, request.Customer)
            .MapErrorAsync(MapError);
    }

    public Task<Result<PublishCouponResponse, SystemError>> PublishCouponAsync(PublishCouponRequest request)
        => RunAs(TestUser.Admin).Coupons().PublishCouponAsync(request)
            .MapErrorAsync(MapError)
            .MapAsync(_ => new PublishCouponResponse());

    public Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync(BrowseCouponsRequest request)
        => RunAs(TestUser.Admin).Coupons().BrowseCouponsAsync(request.Page, request.Size)
            .MapErrorAsync(MapError);

    public Task<Result<ViewProfileResponse, SystemError>> ViewProfileAsync(ViewProfileRequest request)
    {
        RequireNotSet("whenAskedToSignIn", request.WhenAskedToSignIn);
        return RunAs(TestUser.Customer).Profile().ViewProfileAsync()
            .MapErrorAsync(MapError);
    }

    public Task<Result<ViewProfileResponse, SystemError>> UpdateProfileAsync(UpdateProfileRequest request)
        => RunAs(TestUser.Customer).Profile().UpdateProfileAsync(request)
            .MapErrorAsync(MapError);

    /// <summary>The catalog is public: a visitor browses it without signing in, unless a user was explicitly requested.</summary>
    public Task<Result<BrowseCatalogResponse, SystemError>> BrowseCatalogAsync(BrowseCatalogRequest request)
    {
        RequireNotSet("headerCategory", request.HeaderCategory);
        RequireNotSet("search", request.Search);
        RequireNotSet("category", request.Category);
        RequireNotSet("brands", request.Brands);
        RequireNotSet("sort", request.Sort);
        RequireNotSet("followUp", request.FollowUp);

        var client = _anonymous || _requestedUser == null
            ? _apiClient.As(ApiIdentity.Anonymous)
            : RunAs(TestUser.Customer);
        return client.Products().BrowseProductsAsync()
            .MapErrorAsync(MapError)
            .MapAsync(products => new BrowseCatalogResponse { Products = products, ProductCount = products.Count });
    }

    public Task<Result<ViewProductResponse, SystemError>> ViewProductAsync(ViewProductRequest request)
    {
        RequireNotSet("fromCatalog", request.FromCatalog);
        RequireNotSet("cartSku", request.CartSku);
        RequireNotSet("quantity", request.Quantity);
        RequireNotSet("quantityStep", request.QuantityStep);
        RequireNotSet("addToCart", request.AddToCart);
        RequireNotSet("followUp", request.FollowUp);

        var client = _anonymous || _requestedUser == null
            ? _apiClient.As(ApiIdentity.Anonymous)
            : RunAs(TestUser.Customer);
        return client.Products().ViewProductAsync(request.Sku!)
            .MapErrorAsync(MapError)
            .MapAsync(product => new ViewProductResponse
            {
                Sku = product.Sku,
                Title = product.Title,
                Brand = product.Brand,
                Category = product.Category,
                Description = product.Description,
                Price = product.Price,
            });
    }

    /// <summary>The cart lives in the browser (session, cookie or local storage): there is no cart API.</summary>
    public Task<Result<ManageCartResponse, SystemError>> ManageCartAsync(ManageCartRequest request)
        => throw new NotSupportedException("The API channel has no cart; run this scenario on the UI channel only");

    public Task<Result<CheckOutResponse, SystemError>> CheckOutAsync(CheckOutRequest request)
        => throw new NotSupportedException("The API channel has no cart to check out; run this scenario on the UI channel only");

    /// <summary>The API serves the unfiltered catalog; filtering, search and sort happen in the UI.</summary>
    private static void RequireNotSet(string input, object? value)
    {
        if (value != null)
        {
            throw new NotSupportedException($"The API channel cannot send '{input}'; run this scenario on the UI channel only");
        }
    }
}
