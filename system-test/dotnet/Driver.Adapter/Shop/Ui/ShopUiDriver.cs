using Common;
using Driver.Adapter.Shop.Ui.Client;
using Driver.Adapter.Shop.Ui.Client.Pages;
using Driver.Port.Shop.Dtos;
using Driver.Port.Shop.Dtos.Error;
using Driver.Port.Shop;
using static Driver.Port.Shop.SystemResults;

namespace Driver.Adapter.Shop.Ui;

public class ShopUiDriver : IShopDriver
{
    private readonly ShopUiClient _client;
    private Page _currentPage = Page.None;

    private HomePage? _homePage;
    private NewOrderPage? _newOrderPage;
    private OrderHistoryPage? _orderHistoryPage;
    private OrderDetailsPage? _orderDetailsPage;
    private CouponManagementPage? _couponManagementPage;

    private const int MaxBrowseRetries = 10;
    private static readonly TimeSpan BrowseRetryDelay = TimeSpan.FromSeconds(1);

    private ShopUiDriver(ShopUiClient client)
    {
        _client = client;
    }

    public async ValueTask DisposeAsync()
    {
        if (_client != null)
            await _client.DisposeAsync();
    }

    public static async Task<ShopUiDriver> CreateAsync(string baseUrl)
    {
        var client = await ShopUiClient.CreateAsync(baseUrl);
        return new ShopUiDriver(client);
    }

    public async Task<Result<VoidValue, SystemError>> GoToShopAsync()
    {
        _homePage = await _client.OpenHomePageAsync();

        if (!_client.IsStatusOk() || !await _client.IsPageLoadedAsync())
        {
            return Failure("Failed to load home page");
        }

        SetCurrentPage(Page.Home);

        return Success();
    }

    public async Task<Result<PlaceOrderResponse, SystemError>> PlaceOrderAsync(PlaceOrderRequest request)
    {
        var sku = request.Sku;
        var quantity = request.Quantity;
        var country = request.Country;
        var couponCode = request.CouponCode;

        await EnsureOnNewOrderPageAsync();

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

    public async Task<Result<VoidValue, SystemError>> CancelOrderAsync(string? orderNumber)
    {
        var viewResult = await ViewOrderAsync(orderNumber);
        if (viewResult.IsFailure)
        {
            return viewResult.MapVoid();
        }

        await _orderDetailsPage!.ClickCancelOrderAsync();

        var cancelResult = await _orderDetailsPage.GetResultAsync();
        if (cancelResult.IsFailure)
        {
            return Failure(cancelResult.Error);
        }

        var successMessage = cancelResult.Value;
        if (!successMessage.Contains("cancelled successfully"))
        {
            return Failure("Did not receive expected cancellation success message");
        }

        var displayStatusAfterCancel = await _orderDetailsPage.GetStatusAsync();
        if (displayStatusAfterCancel != OrderStatus.Cancelled)
        {
            return Failure("Order status not updated to CANCELLED");
        }

        if (!await _orderDetailsPage.IsCancelButtonHiddenAsync())
        {
            return Failure("Cancel button still visible");
        }

        return Success();
    }

    public async Task<Result<VoidValue, SystemError>> DeliverOrderAsync(string? orderNumber)
    {
        var viewResult = await ViewOrderAsync(orderNumber);
        if (viewResult.IsFailure)
        {
            return viewResult.MapVoid();
        }

        await _orderDetailsPage!.ClickDeliverOrderAsync();

        var deliverResult = await _orderDetailsPage.GetResultAsync();
        if (deliverResult.IsFailure)
        {
            return Failure(deliverResult.Error);
        }

        var successMessage = deliverResult.Value;
        if (!successMessage.Contains("delivered successfully"))
        {
            return Failure("Did not receive expected delivery success message");
        }

        var displayStatusAfterDeliver = await _orderDetailsPage.GetStatusAsync();
        if (displayStatusAfterDeliver != OrderStatus.Delivered)
        {
            return Failure("Order status not updated to DELIVERED");
        }

        return Success();
    }

    public async Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(string? orderNumber)
    {
        var result = await EnsureOnOrderDetailsPageAsync(orderNumber);
        if (result.IsFailure)
        {
            return Failure<ViewOrderResponse>(result.Error);
        }

        var isSuccess = await _orderDetailsPage!.IsLoadedSuccessfullyAsync();
        if (!isSuccess)
        {
            return Failure<ViewOrderResponse>(result.Error);
        }

        var displayOrderNumber = await _orderDetailsPage.GetOrderNumberAsync();
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

    public async Task<Result<VoidValue, SystemError>> PublishCouponAsync(PublishCouponRequest request)
    {
        await EnsureOnCouponManagementPageAsync();

        await _couponManagementPage!.InputCouponCodeAsync(request.Code);
        await _couponManagementPage.InputDiscountRateAsync(request.DiscountRate);
        await _couponManagementPage.InputValidFromAsync(request.ValidFrom);
        await _couponManagementPage.InputValidToAsync(request.ValidTo);
        await _couponManagementPage.InputUsageLimitAsync(request.UsageLimit);
        await _couponManagementPage.ClickPublishCouponAsync();

        var result = await _couponManagementPage.GetResultAsync();
        return result.MapVoid();
    }

    public async Task<Result<BrowseCouponsResponse, SystemError>> BrowseCouponsAsync()
    {
        // Retry with fresh page navigations to handle UI eventual consistency —
        // after publishing a coupon or placing an order, the coupon table
        // may not yet reflect the latest state on first load.
        for (int attempt = 0; attempt < MaxBrowseRetries; attempt++)
        {
            await NavigateToCouponManagementPageAsync();
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

    private async Task<HomePage> GetHomePageAsync()
    {
        if (_homePage == null || !IsOnPage(Page.Home))
        {
            _homePage = await _client.OpenHomePageAsync();
            SetCurrentPage(Page.Home);
        }
        return _homePage;
    }

    private async Task EnsureOnNewOrderPageAsync()
    {
        if (!IsOnPage(Page.NewOrder))
        {
            var homePage = await GetHomePageAsync();
            _newOrderPage = await homePage.ClickNewOrderAsync();
            SetCurrentPage(Page.NewOrder);
        }
    }

    private async Task EnsureOnOrderHistoryPageAsync()
    {
        if (!IsOnPage(Page.OrderHistory))
        {
            var homePage = await GetHomePageAsync();
            _orderHistoryPage = await homePage.ClickOrderHistoryAsync();
            SetCurrentPage(Page.OrderHistory);
        }
    }

    private async Task<Result<VoidValue, SystemError>> EnsureOnOrderDetailsPageAsync(string? orderNumber)
    {
        await EnsureOnOrderHistoryPageAsync();

        await _orderHistoryPage!.InputOrderNumberAsync(orderNumber);
        await _orderHistoryPage.ClickSearchAsync();

        var isOrderListed = await _orderHistoryPage.WaitForOrderRowAsync(orderNumber);
        if (!isOrderListed)
        {
            return Failure("Order " + orderNumber + " does not exist.");
        }

        _orderDetailsPage = await _orderHistoryPage.ClickViewOrderDetailsAsync(orderNumber);
        SetCurrentPage(Page.OrderDetails);

        return Success();
    }

    private async Task EnsureOnCouponManagementPageAsync()
    {
        if (!IsOnPage(Page.CouponManagement))
        {
            await NavigateToCouponManagementPageAsync();
        }
    }

    private async Task NavigateToCouponManagementPageAsync()
    {
        var homePage = await GetHomePageAsync();
        _couponManagementPage = await homePage.ClickCouponManagementAsync();
        SetCurrentPage(Page.CouponManagement);
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
