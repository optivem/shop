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

        await EnsureOnNewOrderPageAsync();

        await _newOrderPage!.InputSkuAsync(sku);
        await _newOrderPage.InputQuantityAsync(quantity);
        await _newOrderPage.InputCountryAsync(country);
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
        var unitPrice = await _orderDetailsPage.GetUnitPriceAsync();
        var totalPrice = await _orderDetailsPage.GetTotalPriceAsync();
        var status = await _orderDetailsPage.GetStatusAsync();

        var response = new ViewOrderResponse
        {
            OrderNumber = displayOrderNumber,
            OrderTimestamp = orderTimestamp.DateTime,
            Sku = sku,
            Quantity = quantity,
            UnitPrice = unitPrice,
            TotalPrice = totalPrice,
            Status = status,
        };

        return Success(response);
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
        OrderDetails
    }

}

