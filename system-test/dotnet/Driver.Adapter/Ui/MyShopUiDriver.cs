using Common;
using Driver.Adapter.Shared.Client.Http;
using Driver.Adapter.Ui.Client;
using Driver.Adapter.Ui.Client.Pages;
using Driver.Port.Dtos;
using Driver.Port.Dtos.Error;
using Driver.Port;
using Common.Domain;
using static Driver.Port.SystemResults;

namespace Driver.Adapter.Ui;

public class MyShopUiDriver : IMyShopDriver
{
    private readonly MyShopUiClient _client;
    private TestUser? _requestedUser;
    private bool _anonymous;
    private Page _currentPage = Page.None;

    private HomePage? _homePage;
    private NewOrderPage? _newOrderPage;
    private OrderHistoryPage? _orderHistoryPage;
    private OrderDetailsPage? _orderDetailsPage;
    private CouponManagementPage? _couponManagementPage;

    private const int MaxBrowseRetries = 10;
    private static readonly TimeSpan BrowseRetryDelay = TimeSpan.FromSeconds(1);

    private MyShopUiDriver(MyShopUiClient client)
    {
        _client = client;
    }

    public async ValueTask DisposeAsync()
    {
        if (_client != null)
            await _client.DisposeAsync();
        GC.SuppressFinalize(this);
    }

    public static async Task<MyShopUiDriver> CreateAsync(string baseUrl, string? keycloakBaseUrl = null)
    {
        var client = await MyShopUiClient.CreateAsync(baseUrl, keycloakBaseUrl);
        return new MyShopUiDriver(client);
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

    public async Task<Result<GoToMyShopResponse, SystemError>> GoToMyShopAsync(GoToMyShopRequest request)
    {
        _homePage = await _client.OpenHomePageAsync();

        if (!_client.IsStatusOk() || !await _client.IsPageLoadedAsync())
        {
            return Failure<GoToMyShopResponse>("Failed to load home page");
        }

        SetCurrentPage(Page.Home);

        return Success(new GoToMyShopResponse());
    }

    public async Task<Result<PlaceOrderResponse, SystemError>> PlaceOrderAsync(PlaceOrderRequest request)
    {
        if (_anonymous)
        {
            return await LoginRequiredAsync<PlaceOrderResponse>("/new-order");
        }

        await RunAsAsync(TestUser.Customer);
        var sku = request.Sku;
        var quantity = request.Quantity;
        var country = request.Country;
        var couponCode = request.CouponCode;

        var pageResult = await EnsureOnNewOrderPageAsync();
        if (pageResult.IsFailure)
        {
            return Failure<PlaceOrderResponse>(pageResult.Error);
        }

        await _newOrderPage!.InputSkuAsync(sku);
        await _newOrderPage.InputQuantityAsync(quantity);
        await _newOrderPage.InputCountryAsync(country);
        await _newOrderPage.InputCouponCodeAsync(couponCode);
        await _newOrderPage.ClickPlaceOrderAsync();

        var result = await _newOrderPage.GetResultAsync();
        if (result.IsFailure)
        {
            return Failure<PlaceOrderResponse>(result.Error);
        }

        var orderNumberValue = NewOrderPage.GetOrderNumber(result.Value);
        var response = new PlaceOrderResponse
        {
            OrderNumber = orderNumberValue
        };

        return Success(response);
    }

    public async Task<Result<CancelOrderResponse, SystemError>> CancelOrderAsync(CancelOrderRequest request)
    {
        await RunAsAsync(TestUser.Customer);
        var viewResult = await ViewOrderAsync(new ViewOrderRequest { OrderNumber = request.OrderNumber });
        if (viewResult.IsFailure)
        {
            return Failure<CancelOrderResponse>(viewResult.Error);
        }

        await _orderDetailsPage!.ClickCancelOrderAsync();

        var cancelResult = await _orderDetailsPage.GetResultAsync();
        if (cancelResult.IsFailure)
        {
            return Failure<CancelOrderResponse>(cancelResult.Error);
        }

        var successMessage = cancelResult.Value;
        if (!successMessage.Contains("cancelled successfully"))
        {
            return Failure<CancelOrderResponse>("Did not receive expected cancellation success message");
        }

        var displayStatusAfterCancel = await _orderDetailsPage.GetStatusAsync();
        if (displayStatusAfterCancel != OrderStatus.Cancelled)
        {
            return Failure<CancelOrderResponse>("Order status not updated to CANCELLED");
        }

        if (!await _orderDetailsPage.IsCancelButtonHiddenAsync())
        {
            return Failure<CancelOrderResponse>("Cancel button still visible");
        }

        return Success(new CancelOrderResponse());
    }

    public async Task<Result<DeliverOrderResponse, SystemError>> DeliverOrderAsync(DeliverOrderRequest request)
    {
        await RunAsAsync(TestUser.Admin);
        var viewResult = await ViewOrderAsync(new ViewOrderRequest { OrderNumber = request.OrderNumber });
        if (viewResult.IsFailure)
        {
            return Failure<DeliverOrderResponse>(viewResult.Error);
        }

        if (await _orderDetailsPage!.IsDeliverButtonHiddenAsync() && viewResult.Value.Status == OrderStatus.Placed)
        {
            return Failure<DeliverOrderResponse>(MyShopUiClient.PermissionDenied);
        }

        await _orderDetailsPage.ClickDeliverOrderAsync();

        var deliverResult = await _orderDetailsPage.GetResultAsync();
        if (deliverResult.IsFailure)
        {
            return Failure<DeliverOrderResponse>(deliverResult.Error);
        }

        var successMessage = deliverResult.Value;
        if (!successMessage.Contains("delivered successfully"))
        {
            return Failure<DeliverOrderResponse>("Did not receive expected delivery success message");
        }

        var displayStatusAfterDeliver = await _orderDetailsPage.GetStatusAsync();
        if (displayStatusAfterDeliver != OrderStatus.Delivered)
        {
            return Failure<DeliverOrderResponse>("Order status not updated to DELIVERED");
        }

        return Success(new DeliverOrderResponse());
    }

    public async Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(ViewOrderRequest request)
    {
        if (_anonymous)
        {
            return await LoginRequiredAsync<ViewOrderResponse>($"/order-details?orderNumber={Uri.EscapeDataString(request.OrderNumber ?? string.Empty)}");
        }

        await RunAsAsync(TestUser.Customer);
        var result = await EnsureOnOrderDetailsPageAsync(request.OrderNumber);
        if (result.IsFailure)
        {
            return Failure<ViewOrderResponse>(result.Error);
        }

        var displayOrderNumber = await _orderDetailsPage!.GetOrderNumberAsync();
        var orderTimestamp = await _orderDetailsPage.GetOrderTimestampAsync();
        var sku = await _orderDetailsPage.GetSkuAsync();
        var quantity = await _orderDetailsPage.GetQuantityAsync();
        var country = await _orderDetailsPage.GetCountryAsync();
        var unitPrice = await _orderDetailsPage.GetUnitPriceAsync();
        var basePrice = await _orderDetailsPage.GetBasePriceAsync();
        var discountRate = await _orderDetailsPage.GetDiscountRateAsync();
        var discountAmount = await _orderDetailsPage.GetDiscountAmountAsync();
        var subtotalPrice = await _orderDetailsPage.GetSubtotalPriceAsync();
        var taxRate = await _orderDetailsPage.GetTaxRateAsync();
        var taxAmount = await _orderDetailsPage.GetTaxAmountAsync();
        var totalPrice = await _orderDetailsPage.GetTotalPriceAsync();
        var status = await _orderDetailsPage.GetStatusAsync();
        var appliedCouponCode = await _orderDetailsPage.GetAppliedCouponAsync();

        var response = new ViewOrderResponse
        {
            OrderNumber = displayOrderNumber,
            OrderTimestamp = orderTimestamp.DateTime,
            Sku = sku,
            Quantity = quantity,
            Country = country,
            UnitPrice = unitPrice,
            BasePrice = basePrice,
            DiscountRate = discountRate,
            DiscountAmount = discountAmount,
            SubtotalPrice = subtotalPrice,
            TaxRate = taxRate,
            TaxAmount = taxAmount,
            TotalPrice = totalPrice,
            Status = status,
            AppliedCouponCode = appliedCouponCode
        };

        return Success(response);
    }

    public async Task<Result<BrowseOrderHistoryResponse, SystemError>> BrowseOrderHistoryAsync(BrowseOrderHistoryRequest request)
    {
        if (_anonymous)
        {
            return await LoginRequiredAsync<BrowseOrderHistoryResponse>("/order-history");
        }

        await RunAsAsync(TestUser.Customer);
        var homePage = await GetHomePageAsync();
        _orderHistoryPage = await homePage.ClickOrderHistoryAsync();
        SetCurrentPage(Page.OrderHistory);

        if (!string.IsNullOrWhiteSpace(request.OrderNumber))
        {
            await _orderHistoryPage.InputOrderNumberAsync(request.OrderNumber);
            await _orderHistoryPage.ClickSearchAsync();
        }

        var orderNumbers = await _orderHistoryPage.ReadOrderNumbersAsync();
        var orders = orderNumbers
            .Select(orderNumber => new BrowseOrderHistoryResponse.OrderDto { OrderNumber = orderNumber })
            .ToList();

        return Success(new BrowseOrderHistoryResponse { Orders = orders });
    }

    public async Task<Result<PublishCouponResponse, SystemError>> PublishCouponAsync(PublishCouponRequest request)
    {
        if (_anonymous)
        {
            return await LoginRequiredAsync<PublishCouponResponse>("/admin-coupons");
        }

        await RunAsAsync(TestUser.Admin);
        var pageResult = await EnsureOnCouponManagementPageAsync();
        if (pageResult.IsFailure)
        {
            return Failure<PublishCouponResponse>(pageResult.Error);
        }

        await _couponManagementPage!.InputCouponCodeAsync(request.Code);
        await _couponManagementPage.InputDiscountRateAsync(request.DiscountRate);
        await _couponManagementPage.InputValidFromAsync(request.ValidFrom);
        await _couponManagementPage.InputValidToAsync(request.ValidTo);
        await _couponManagementPage.InputUsageLimitAsync(request.UsageLimit);
        await _couponManagementPage.ClickPublishCouponAsync();

        var result = await _couponManagementPage.GetResultAsync();
        return result.Map(_ => new PublishCouponResponse());
    }

    public async Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync(BrowseCouponsRequest request)
    {
        if (_anonymous)
        {
            return await LoginRequiredAsync<BrowseCouponsResponse>("/admin-coupons");
        }

        await RunAsAsync(TestUser.Admin);

        // Retry with fresh page navigations to handle UI eventual consistency —
        // after publishing a coupon or placing an order, the coupon table
        // may not yet reflect the latest state on first load.
        for (int attempt = 0; attempt < MaxBrowseRetries; attempt++)
        {
            var pageResult = await NavigateToCouponManagementPageAsync();
            if (pageResult.IsFailure)
            {
                return Failure<BrowseCouponsResponse>(pageResult.Error);
            }

            var coupons = await _couponManagementPage!.ReadCouponsAsync();

            if (coupons.Count > 0)
            {
                return Success(new BrowseCouponsResponse { Coupons = coupons });
            }

            await Task.Delay(BrowseRetryDelay);
        }

        // Final attempt — return whatever we get (even if empty)
        await NavigateToCouponManagementPageAsync();
        var finalCoupons = await _couponManagementPage!.ReadCouponsAsync();

        return Success(new BrowseCouponsResponse { Coupons = finalCoupons });
    }

    /// <summary>An explicitly requested identity wins; otherwise the operation runs as its default user.</summary>
    private async Task RunAsAsync(TestUser operationDefault)
    {
        var user = _requestedUser ?? operationDefault;
        if (await _client.SwitchUserAsync(user))
        {
            _currentPage = Page.None;
            _homePage = null;
            _newOrderPage = null;
            _orderHistoryPage = null;
            _orderDetailsPage = null;
            _couponManagementPage = null;
        }
    }

    private async Task<Result<T, SystemError>> LoginRequiredAsync<T>(string path)
    {
        await _client.AssertLoginRequiredForAsync(path);
        return Failure<T>(MyShopUiClient.AuthenticationRequired);
    }

    private async Task<HomePage> GetHomePageAsync()
    {
        if (_homePage == null || !IsOnPage(Page.Home))
        {
            _homePage = await _client.OpenHomePageAsync();
            SetCurrentPage(Page.Home);
        }
        return _homePage;
    }

    /// <summary>A user who has no link to the page opens its URL, which is what can be refused.</summary>
    private async Task<Result<VoidValue, SystemError>> EnsureOnNewOrderPageAsync()
    {
        if (IsOnPage(Page.NewOrder))
        {
            return Success();
        }

        var homePage = await GetHomePageAsync();
        if (_requestedUser != TestUser.Admin)
        {
            _newOrderPage = await homePage.ClickNewOrderAsync();
            SetCurrentPage(Page.NewOrder);
            return Success();
        }

        var result = await _client.OpenNewOrderPageAsync();
        if (result.IsFailure)
        {
            SetCurrentPage(Page.None);
            return Failure(result.Error);
        }

        _newOrderPage = result.Value;
        SetCurrentPage(Page.NewOrder);
        return Success();
    }

    private async Task<Result<VoidValue, SystemError>> EnsureOnOrderDetailsPageAsync(string? orderNumber)
    {
        await GetHomePageAsync();
        _orderDetailsPage = await _client.OpenOrderDetailsPageAsync(orderNumber);
        SetCurrentPage(Page.OrderDetails);

        return await _orderDetailsPage.GetLoadResultAsync(orderNumber);
    }

    private async Task<Result<VoidValue, SystemError>> EnsureOnCouponManagementPageAsync()
    {
        return IsOnPage(Page.CouponManagement) ? Success() : await NavigateToCouponManagementPageAsync();
    }

    /// <summary>A user who has no link to the page opens its URL, which is what can be refused.</summary>
    private async Task<Result<VoidValue, SystemError>> NavigateToCouponManagementPageAsync()
    {
        var homePage = await GetHomePageAsync();
        if (_requestedUser == null || _requestedUser == TestUser.Admin)
        {
            _couponManagementPage = await homePage.ClickCouponManagementAsync();
            SetCurrentPage(Page.CouponManagement);
            return Success();
        }

        var result = await _client.OpenCouponManagementPageAsync();
        if (result.IsFailure)
        {
            SetCurrentPage(Page.None);
            return Failure(result.Error);
        }

        _couponManagementPage = result.Value;
        SetCurrentPage(Page.CouponManagement);
        return Success();
    }

    private bool IsOnPage(Page page)
    {
        return _currentPage == page;
    }

    private void SetCurrentPage(Page page)
    {
        _currentPage = page;
    }

    private enum Page
    {
        None,
        Home,
        NewOrder,
        OrderHistory,
        OrderDetails,
        CouponManagement
    }
}
