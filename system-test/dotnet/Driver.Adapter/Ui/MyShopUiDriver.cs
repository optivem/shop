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
            UserIdentityKind.Customer => TestUser.ForCustomer(identity),
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
        RequireNotSet("Idempotency-Key", request.IdempotencyKey);
        RequireNotSet("shippingAddress", request.ShippingAddress);
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
        if (_anonymous)
        {
            return await LoginRequiredForOrderAsync<CancelOrderResponse>(request.OrderNumber);
        }

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

        if (!await _orderDetailsPage.IsCancelButtonHiddenAsync())
        {
            return Failure<CancelOrderResponse>("Cancel button still visible");
        }

        return Success(new CancelOrderResponse());
    }

    public async Task<Result<DeliverOrderResponse, SystemError>> DeliverOrderAsync(DeliverOrderRequest request)
    {
        if (_anonymous)
        {
            return await LoginRequiredForOrderAsync<DeliverOrderResponse>(request.OrderNumber);
        }

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

        return Success(new DeliverOrderResponse());
    }

    public async Task<Result<ViewOrderResponse, SystemError>> ViewOrderAsync(ViewOrderRequest request)
    {
        if (_anonymous)
        {
            return await LoginRequiredForOrderAsync<ViewOrderResponse>(request.OrderNumber);
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
        RequireNotSet("page", request.Page);
        RequireNotSet("size", request.Size);
        RequireNotSet("customer", request.Customer);
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
        RequireNotSet("page", request.Page);
        RequireNotSet("size", request.Size);
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
            ResetPages();
        }
    }

    /// <summary>The browser session was replaced: no page is open any more.</summary>
    private void ResetPages()
    {
        _currentPage = Page.None;
        _homePage = null;
        _newOrderPage = null;
        _orderHistoryPage = null;
        _orderDetailsPage = null;
        _couponManagementPage = null;
    }

    /// <summary>The catalog is public: a visitor browses it without signing in, unless a user was explicitly requested.</summary>
    public async Task<Result<BrowseCatalogResponse, SystemError>> BrowseCatalogAsync(BrowseCatalogRequest request)
    {
        CatalogPage catalogPage;
        if (_anonymous || _requestedUser == null)
        {
            var anonymousCatalogPage = await _client.OpenCatalogAnonymouslyAsync();
            ResetPages();
            if (anonymousCatalogPage == null)
            {
                return Failure<BrowseCatalogResponse>(MyShopUiClient.AuthenticationRequired);
            }
            catalogPage = anonymousCatalogPage;
        }
        else
        {
            await RunAsAsync(TestUser.Customer);
            catalogPage = await _client.OpenCatalogAsync();
            SetCurrentPage(Page.None);
        }
        await catalogPage.WaitForLoadedAsync();

        if (request.HeaderCategory != null)
        {
            await catalogPage.ClickHeaderCategoryAsync(request.HeaderCategory);
        }
        if (request.Search != null)
        {
            await catalogPage.SearchAsync(request.Search);
        }
        if (request.Category != null)
        {
            await catalogPage.SelectCategoryAsync(request.Category);
        }
        if (request.Brands != null)
        {
            var brands = request.Brands.Split(BrowseCatalogRequest.BrandSeparator)
                .Select(brand => brand.Trim())
                .Where(brand => brand.Length > 0);
            foreach (var brand in brands)
            {
                await catalogPage.TickBrandAsync(brand);
            }
        }
        if (request.Sort != null)
        {
            await catalogPage.SortByAsync(request.Sort);
        }
        switch (request.FollowUp)
        {
            case null:
                break;
            case BrowseCatalogRequest.ReopenPageAddress:
                await catalogPage.ReopenPageAddressAsync();
                break;
            case BrowseCatalogRequest.ClearFilters:
                await catalogPage.ClickClearFiltersAsync();
                break;
            default:
                throw new ArgumentException($"Unknown catalog follow-up: {request.FollowUp}", nameof(request));
        }

        return Success(await catalogPage.ReadAsync());
    }

    /// <summary>
    /// Product pages are public: a visitor views them without signing in, unless a user was explicitly requested.
    /// The visitor's cart line, if any, is added first, from that product's page, in the same session.
    /// </summary>
    public async Task<Result<ViewProductResponse, SystemError>> ViewProductAsync(ViewProductRequest request)
    {
        if (_anonymous || _requestedUser == null)
        {
            await _client.StartVisitorSessionAsync();
        }
        else
        {
            await RunAsAsync(TestUser.Customer);
        }
        ResetPages();

        if (request.CartSku != null)
        {
            var cartProductPage = await _client.OpenProductPageAsync(request.CartSku);
            if (cartProductPage == null)
            {
                return Failure<ViewProductResponse>(MyShopUiClient.AuthenticationRequired);
            }
            await cartProductPage.WaitForLoadedAsync();
            await cartProductPage.InputQuantityAsync(request.CartQuantity);
            await cartProductPage.ClickAddToCartAsync();
        }

        ProductPage? productPage;
        if (request.FromCatalog == true)
        {
            var catalogPage = await _client.OpenCatalogInSessionAsync();
            if (catalogPage == null)
            {
                return Failure<ViewProductResponse>(MyShopUiClient.AuthenticationRequired);
            }
            await catalogPage.WaitForLoadedAsync();
            productPage = await catalogPage.ClickProductAsync(request.Sku!);
        }
        else
        {
            productPage = await _client.OpenProductPageAsync(request.Sku!);
            if (productPage == null)
            {
                return Failure<ViewProductResponse>(MyShopUiClient.AuthenticationRequired);
            }
        }
        await productPage.WaitForLoadedAsync();

        if (request.Quantity != null)
        {
            await productPage.InputQuantityAsync(request.Quantity);
        }
        switch (request.QuantityStep)
        {
            case null:
                break;
            case ViewProductRequest.Increase:
                await productPage.ClickIncreaseQuantityAsync();
                break;
            case ViewProductRequest.Decrease:
                await productPage.ClickDecreaseQuantityAsync();
                break;
            default:
                throw new ArgumentException($"Unknown quantity step: {request.QuantityStep}", nameof(request));
        }
        var addToCart = request.AddToCart == true;
        if (addToCart)
        {
            await productPage.ClickAddToCartAsync();
        }

        var response = await productPage.ReadAsync();

        switch (request.FollowUp)
        {
            case null:
                break;
            case ViewProductRequest.BackToCatalog:
                var catalog = await (await productPage.ClickBackToCatalogAsync()).ReadAsync();
                response.CatalogSkus = catalog.Products!.Select(product => product.Sku!).ToList();
                break;
            default:
                throw new ArgumentException($"Unknown product follow-up: {request.FollowUp}", nameof(request));
        }
        if (addToCart)
        {
            response.CartLines = await (await _client.OpenCartPageAsync()).ReadLinesAsync();
        }

        return Success(response);
    }

    /// <summary>
    /// The cart is public: a visitor keeps one without signing in, unless a user was explicitly requested. A new
    /// session first gets the cart lines, each from its product page; then the action runs, and the cart is read.
    /// </summary>
    public async Task<Result<ManageCartResponse, SystemError>> ManageCartAsync(ManageCartRequest request)
    {
        if (request.ContinueSession != true)
        {
            var filled = await StartCartSessionAsync(request.CartLines);
            if (filled.IsFailure)
            {
                return Failure<ManageCartResponse>(filled.Error);
            }
        }
        var action = request.Action;
        if (action == null)
        {
            return Success(new ManageCartResponse());
        }

        string? message = null;
        int? cartItemCount = null;
        CartPage? cartPage = null;
        switch (action)
        {
            case ManageCartRequest.ViewCart:
                // The cart is read below.
                break;
            case ManageCartRequest.BrowseCatalog:
            case ManageCartRequest.AddFromCatalog:
            {
                var catalogPage = await _client.OpenCatalogInSessionAsync();
                if (catalogPage == null)
                {
                    return Failure<ManageCartResponse>(MyShopUiClient.AuthenticationRequired);
                }
                await catalogPage.WaitForLoadedAsync();
                if (action == ManageCartRequest.AddFromCatalog)
                {
                    message = await catalogPage.ClickAddToCartAsync(request.Sku!);
                }
                cartItemCount = await catalogPage.ReadCartItemCountAsync();
                break;
            }
            case ManageCartRequest.AddFromProductPage:
            {
                var productPage = await _client.OpenProductPageAsync(request.Sku!);
                if (productPage == null)
                {
                    return Failure<ManageCartResponse>(MyShopUiClient.AuthenticationRequired);
                }
                await productPage.WaitForLoadedAsync();
                await productPage.InputQuantityAsync(request.Quantity);
                await productPage.ClickAddToCartAsync();
                var shown = await productPage.ReadAsync();
                message = shown.Message;
                cartItemCount = shown.CartItemCount;
                break;
            }
            case ManageCartRequest.ChangeQuantity:
            case ManageCartRequest.IncreaseQuantity:
            case ManageCartRequest.DecreaseQuantity:
            case ManageCartRequest.RemoveLine:
            {
                cartPage = await _client.OpenCartPageAsync();
                await cartPage.WaitForLoadedAsync();
                switch (action)
                {
                    case ManageCartRequest.ChangeQuantity:
                        await cartPage.InputQuantityAsync(request.Sku!, request.Quantity);
                        break;
                    case ManageCartRequest.IncreaseQuantity:
                        await cartPage.ClickIncreaseQuantityAsync(request.Sku!);
                        break;
                    case ManageCartRequest.DecreaseQuantity:
                        await cartPage.ClickDecreaseQuantityAsync(request.Sku!);
                        break;
                    default:
                        await cartPage.ClickRemoveAsync(request.Sku!);
                        break;
                }
                message = await cartPage.ReadNotificationAsync();
                break;
            }
            case ManageCartRequest.Revisit:
            case ManageCartRequest.SignIn:
            {
                if (action == ManageCartRequest.Revisit)
                {
                    await _client.RevisitAsync();
                }
                var catalogPage = await _client.OpenCatalogInSessionAsync();
                if (catalogPage == null)
                {
                    return Failure<ManageCartResponse>(MyShopUiClient.AuthenticationRequired);
                }
                await catalogPage.WaitForLoadedAsync();
                if (action == ManageCartRequest.SignIn)
                {
                    await _client.SignInAsync(TestUser.Customer);
                }
                cartItemCount = await _client.CurrentPageInSession().ReadCartItemCountAsync();
                break;
            }
            default:
                throw new ArgumentException($"Unknown cart action: {action}", nameof(request));
        }

        cartPage ??= await _client.OpenCartPageAsync();
        var response = await cartPage.ReadAsync(message, cartItemCount);
        response.AskedToSignIn = _client.IsOnLoginPage();
        return Success(response);
    }

    /// <summary>
    /// Checkout is for signed-in customers: a visitor is asked to sign in, an admin is refused. A new session first
    /// gets the cart lines, each from its product page; then the checkout is opened, and, when asked, the order placed.
    /// </summary>
    public async Task<Result<CheckOutResponse, SystemError>> CheckOutAsync(CheckOutRequest request)
    {
        if (request.ContinueSession != true)
        {
            var lines = (request.CartLines ?? [])
                .Select(line => new ManageCartRequest.CartLineDto { Sku = line.Sku, Quantity = line.Quantity })
                .ToList();
            var filled = await StartCartSessionAsync(lines);
            if (filled.IsFailure)
            {
                return Failure<CheckOutResponse>(filled.Error);
            }
        }

        switch (request.Entry)
        {
            case CheckOutRequest.ProceedFromCart:
                await _client.ProceedToCheckoutFromCartAsync();
                break;
            case null:
            case CheckOutRequest.Open:
                await _client.OpenCheckoutPageAsync();
                break;
            default:
                throw new ArgumentException($"Unknown checkout entry: {request.Entry}", nameof(request));
        }

        if (_client.IsOnLoginPage())
        {
            if (request.SignIn != true)
            {
                return Failure<CheckOutResponse>(MyShopUiClient.AuthenticationRequired);
            }
            await _client.SignInOnLoginFormAsync(TestUser.Customer);
        }
        if (await _client.IsRefusedAsync())
        {
            return Failure<CheckOutResponse>(MyShopUiClient.PermissionDenied);
        }

        var pageClient = _client.SessionPageClient();
        var checkoutPage = new CheckoutPage(pageClient);
        var response = new CheckOutResponse { MessageDetails = [] };
        if (!await checkoutPage.IsShownAsync())
        {
            return Success(await ReadWhereTheCheckoutSentTheCustomerAsync(response));
        }

        response.OpenedTaxCountry = await checkoutPage.ReadTaxCountryAsync();
        response.OpenedEstimatedTaxRate = await checkoutPage.ReadEstimatedTaxRateAsync();
        if (request.ShippingAddress != null)
        {
            await checkoutPage.InputShippingAddressAsync(request.ShippingAddress);
        }
        if (request.SaveAsDefault == false)
        {
            await checkoutPage.SetSaveAsDefaultAsync(false);
        }
        if (request.CouponCode != null)
        {
            await checkoutPage.InputCouponCodeAsync(request.CouponCode);
        }
        if (request.PlaceOrder != true)
        {
            response.ShownPage = CheckOutResponse.Checkout;
            await checkoutPage.ReadIntoAsync(response);
            return Success(response);
        }

        await checkoutPage.ClickPlaceOrderAsync();
        var confirmationPage = new CheckoutConfirmationPage(pageClient);
        if (await confirmationPage.IsShownAsync())
        {
            response.ShownPage = CheckOutResponse.Confirmation;
            await confirmationPage.ReadIntoAsync(response);
        }
        else
        {
            response.ShownPage = CheckOutResponse.Checkout;
            await checkoutPage.ReadIntoAsync(response);
            response.ConfirmedOrders = [];
        }
        response.CartLines = await (await _client.OpenCartPageAsync()).ReadLinesAsync();
        return Success(response);
    }

    /// <summary>The checkout was not shown: the app sent the customer to the cart, or to some other page.</summary>
    private async Task<CheckOutResponse> ReadWhereTheCheckoutSentTheCustomerAsync(CheckOutResponse response)
    {
        var path = _client.CurrentPath();
        if (path.EndsWith("/cart"))
        {
            var cart = await new CartPage(_client.SessionPageClient()).ReadAsync(null, null);
            response.ShownPage = CheckOutResponse.Cart;
            response.Message = cart.Message;
        }
        else
        {
            response.ShownPage = path;
        }
        return response;
    }

    /// <summary>A fresh session (the visitor's, or the requested user's) whose cart gets these lines, in order.</summary>
    private async Task<Result<VoidValue, SystemError>> StartCartSessionAsync(List<ManageCartRequest.CartLineDto>? cartLines)
    {
        if (_anonymous || _requestedUser == null)
        {
            await _client.StartVisitorSessionAsync();
        }
        else
        {
            await RunAsAsync(TestUser.Customer);
        }
        ResetPages();
        foreach (var line in cartLines ?? [])
        {
            var productPage = await _client.OpenProductPageAsync(line.Sku!);
            if (productPage == null)
            {
                return Failure(MyShopUiClient.AuthenticationRequired);
            }
            await productPage.WaitForLoadedAsync();
            await productPage.InputQuantityAsync(line.Quantity);
            await productPage.ClickAddToCartAsync();
        }
        return Success();
    }

    private async Task<Result<T, SystemError>> LoginRequiredAsync<T>(string path)
    {
        await _client.AssertLoginRequiredForAsync(path);
        return Failure<T>(MyShopUiClient.AuthenticationRequired);
    }

    private async Task<Result<T, SystemError>> LoginRequiredForOrderAsync<T>(string? orderNumber)
    {
        var encodedOrderNumber = Uri.EscapeDataString(orderNumber ?? string.Empty);
        await _client.AssertLoginRequiredForAsync($"/order-details?orderNumber={encodedOrderNumber}", $"/order-details/{encodedOrderNumber}");
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

    // The UI has no control for this input (no idempotency key, no paging controls): API-channel scenarios only.
    public Task<Result<ViewProfileResponse, SystemError>> ViewProfileAsync(ViewProfileRequest request)
        => throw new NotSupportedException("Viewing the profile is not supported by the UI channel yet; run this scenario on the API channel only");

    public Task<Result<ViewProfileResponse, SystemError>> UpdateProfileAsync(UpdateProfileRequest request)
        => throw new NotSupportedException("Updating the profile is not supported by the UI channel yet; run this scenario on the API channel only");

    private static void RequireNotSet(string input, object? value)
    {
        if (value != null)
        {
            throw new NotSupportedException($"The UI channel cannot send '{input}'; run this scenario on the API channel only");
        }
    }
}
