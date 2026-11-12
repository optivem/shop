package com.mycompany.myshop.testkit.driver.adapter.ui;

import com.microsoft.playwright.Browser;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.MyShopUiClient;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CartPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CatalogPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CheckoutConfirmationPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CheckoutPage;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CouponManagementPage;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogResponse;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.HomePage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.NewOrderPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderDetailsPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderHistoryPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.ProductPage;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.CancelOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.CancelOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.DeliverOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.DeliverOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.GoToMyShopRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.GoToMyShopResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.PublishCouponRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.PublishCouponResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.UpdateProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;

import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.failure;
import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.success;

public class MyShopUiDriver implements MyShopDriver {
    private final MyShopUiClient client;

    private TestUser requestedUser;
    private boolean anonymous;
    private Page currentPage = Page.NONE;
    private HomePage homePage;
    private NewOrderPage newOrderPage;
    private OrderHistoryPage orderHistoryPage;
    private OrderDetailsPage orderDetailsPage;
    private CouponManagementPage couponManagementPage;

    public MyShopUiDriver(String baseUrl, Browser browser) {
        this(baseUrl, browser, null);
    }

    public MyShopUiDriver(String baseUrl, Browser browser, String keycloakBaseUrl) {
        this.client = new MyShopUiClient(baseUrl, browser, keycloakBaseUrl);
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public void actAs(UserIdentity identity) {
        anonymous = identity.kind() == UserIdentity.Kind.ANONYMOUS;
        requestedUser = switch (identity.kind()) {
            case DEFAULT, ANONYMOUS -> null;
            case CUSTOMER -> TestUser.customer(identity);
            case ADMIN -> TestUser.ADMIN;
        };
    }

    @Override
    public Result<GoToMyShopResponse, SystemError> goToMyShop(GoToMyShopRequest request) {
        homePage = client.openHomePage();

        if (!client.isStatusOk() || !client.isPageLoaded()) {
            return failure("Failed to load home page");
        }

        currentPage = Page.HOME;
        return success(GoToMyShopResponse.builder().build());
    }

    @Override
    public Result<PlaceOrderResponse, SystemError> placeOrder(PlaceOrderRequest request) {
        requireNotSet("Idempotency-Key", request.getIdempotencyKey());
        requireNotSet("shippingAddress", request.getShippingAddress());
        if (anonymous) {
            return loginRequired("/new-order");
        }

        runAs(TestUser.CUSTOMER);
        var sku = request.getSku();
        var quantity = request.getQuantity();
        var country = request.getCountry();
        var couponCode = request.getCouponCode();

        var pageResult = ensureOnNewOrderPage();
        if (pageResult.isFailure()) {
            return failure(pageResult.getError());
        }

        newOrderPage.inputSku(sku);
        newOrderPage.inputQuantity(quantity);
        newOrderPage.inputCountry(country);
        if (couponCode != null && !couponCode.isBlank()) {
            newOrderPage.inputCouponCode(couponCode);
        }
        newOrderPage.clickPlaceOrder();

        var result = newOrderPage.getResult();

        if (result.isFailure()) {
            return failure(result.getError());
        }

        var orderNumberValue = NewOrderPage.getOrderNumber(result.getValue());

        var response = PlaceOrderResponse.builder().orderNumber(orderNumberValue).build();
        return Result.success(response);
    }

    @Override
    public Result<CancelOrderResponse, SystemError> cancelOrder(CancelOrderRequest request) {
        if (anonymous) {
            return loginRequiredForOrder(request.getOrderNumber());
        }

        runAs(TestUser.CUSTOMER);
        var viewResult = viewOrder(ViewOrderRequest.builder().orderNumber(request.getOrderNumber()).build());

        if (viewResult.isFailure()) {
            return failure(viewResult.getError());
        }

        orderDetailsPage.clickCancelOrder();

        var result = orderDetailsPage.getResult();

        if (result.isFailure()) {
            return failure(result.getError());
        }

        return success(CancelOrderResponse.builder().build());
    }

    @Override
    public Result<DeliverOrderResponse, SystemError> deliverOrder(DeliverOrderRequest request) {
        if (anonymous) {
            return loginRequiredForOrder(request.getOrderNumber());
        }

        runAs(TestUser.ADMIN);
        var viewResult = viewOrder(ViewOrderRequest.builder().orderNumber(request.getOrderNumber()).build());

        if (viewResult.isFailure()) {
            return failure(viewResult.getError());
        }

        if (orderDetailsPage.isDeliverButtonHidden() && viewResult.getValue().getStatus() == OrderStatus.PLACED) {
            return failure(MyShopUiClient.PERMISSION_DENIED);
        }

        orderDetailsPage.clickDeliverOrder();

        var result = orderDetailsPage.getResult();

        if (result.isFailure()) {
            return failure(result.getError());
        }

        return success(DeliverOrderResponse.builder().build());
    }

    @Override
    public Result<ViewOrderResponse, SystemError> viewOrder(ViewOrderRequest request) {
        var orderNumber = request.getOrderNumber();
        if (anonymous) {
            return loginRequiredForOrder(orderNumber);
        }

        runAs(TestUser.CUSTOMER);
        var result = ensureOnOrderDetailsPage(orderNumber);
        if (result.isFailure()) {
            return failure(result.getError());
        }

        var displayOrderNumber = orderDetailsPage.getOrderNumber();
        var orderTimestamp = orderDetailsPage.getOrderTimestamp();
        var sku = orderDetailsPage.getSku();
        var quantity = orderDetailsPage.getQuantity();
        var country = orderDetailsPage.getCountry();
        var unitPrice = orderDetailsPage.getUnitPrice();
        var basePrice = orderDetailsPage.getBasePrice();
        var discountRate = orderDetailsPage.getDiscountRate();
        var discountAmount = orderDetailsPage.getDiscountAmount();
        var subtotalPrice = orderDetailsPage.getSubtotalPrice();
        var taxRate = orderDetailsPage.getTaxRate();
        var taxAmount = orderDetailsPage.getTaxAmount();
        var totalPrice = orderDetailsPage.getTotalPrice();
        var status = orderDetailsPage.getStatus();
        var appliedCoupon = orderDetailsPage.getAppliedCoupon();

        var response = ViewOrderResponse.builder()
                .orderNumber(displayOrderNumber)
                .orderTimestamp(orderTimestamp)
                .sku(sku)
                .quantity(quantity)
                .country(country)
                .unitPrice(unitPrice)
                .basePrice(basePrice)
                .discountRate(discountRate)
                .discountAmount(discountAmount)
                .subtotalPrice(subtotalPrice)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .totalPrice(totalPrice)
                .status(status)
                .appliedCouponCode(appliedCoupon)
                .build();

        return success(response);
    }

    @Override
    public Result<BrowseOrderHistoryResponse, SystemError> browseOrderHistory(BrowseOrderHistoryRequest request) {
        requireNotSet("page", request.getPage());
        requireNotSet("size", request.getSize());
        requireNotSet("customer", request.getCustomer());
        if (anonymous) {
            return loginRequired("/order-history");
        }

        runAs(TestUser.CUSTOMER);
        orderHistoryPage = getHomePage().clickOrderHistory();
        currentPage = Page.ORDER_HISTORY;

        var orderNumber = request.getOrderNumber();
        if (orderNumber != null && !orderNumber.isBlank()) {
            orderHistoryPage.inputOrderNumber(orderNumber);
            orderHistoryPage.clickSearch();
        }

        var orders = orderHistoryPage.readOrderNumbers().stream()
                .map(listed -> BrowseOrderHistoryResponse.OrderDto.builder().orderNumber(listed).build())
                .toList();

        return success(BrowseOrderHistoryResponse.builder().orders(orders).build());
    }

    @Override
    public Result<PublishCouponResponse, SystemError> publishCoupon(PublishCouponRequest request) {
        if (anonymous) {
            return loginRequired("/admin-coupons");
        }

        runAs(TestUser.ADMIN);
        var pageResult = ensureOnCouponManagementPage();
        if (pageResult.isFailure()) {
            return failure(pageResult.getError());
        }

        couponManagementPage.inputCouponCode(request.getCode());
        couponManagementPage.inputDiscountRate(request.getDiscountRate());
        couponManagementPage.inputValidFrom(request.getValidFrom());
        couponManagementPage.inputValidTo(request.getValidTo());
        couponManagementPage.inputUsageLimit(request.getUsageLimit());
        couponManagementPage.clickPublishCoupon();

        return couponManagementPage.getResult()
                .map(value -> PublishCouponResponse.builder().build());
    }

    @Override
    public Result<BrowseCouponsResponse, SystemError> browseCoupons(BrowseCouponsRequest request) {
        requireNotSet("page", request.getPage());
        requireNotSet("size", request.getSize());
        if (anonymous) {
            return loginRequired("/admin-coupons");
        }

        runAs(TestUser.ADMIN);
        var pageResult = navigateToCouponManagementPage();
        if (pageResult.isFailure()) {
            return failure(pageResult.getError());
        }

        var coupons = couponManagementPage.readCoupons();

        var response = BrowseCouponsResponse.builder()
                .coupons(coupons)
                .build();

        return success(response);
    }

    @Override
    public Result<ViewProfileResponse, SystemError> viewProfile(ViewProfileRequest request) {
        throw unsupportedOperation("viewProfile");
    }

    @Override
    public Result<ViewProfileResponse, SystemError> updateProfile(UpdateProfileRequest request) {
        throw unsupportedOperation("updateProfile");
    }

    /** The catalog is public: a visitor browses it without signing in, unless a user was explicitly requested. */
    @Override
    public Result<BrowseCatalogResponse, SystemError> browseCatalog(BrowseCatalogRequest request) {
        CatalogPage catalogPage;
        if (anonymous || requestedUser == null) {
            catalogPage = client.openCatalogAnonymously();
            resetPages();
            if (catalogPage == null) {
                return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
            }
        } else {
            runAs(TestUser.CUSTOMER);
            catalogPage = client.openCatalog();
            currentPage = Page.NONE;
        }
        catalogPage.waitForLoaded();

        if (request.getHeaderCategory() != null) {
            catalogPage.clickHeaderCategory(request.getHeaderCategory());
        }
        if (request.getSearch() != null) {
            catalogPage.search(request.getSearch());
        }
        if (request.getCategory() != null) {
            catalogPage.selectCategory(request.getCategory());
        }
        if (request.getBrands() != null) {
            Arrays.stream(request.getBrands().split(BrowseCatalogRequest.BRAND_SEPARATOR))
                    .map(String::trim)
                    .filter(brand -> !brand.isEmpty())
                    .forEach(catalogPage::tickBrand);
        }
        if (request.getSort() != null) {
            catalogPage.sortBy(request.getSort());
        }
        if (BrowseCatalogRequest.REOPEN_PAGE_ADDRESS.equals(request.getFollowUp())) {
            catalogPage.reopenPageAddress();
        } else if (BrowseCatalogRequest.CLEAR_FILTERS.equals(request.getFollowUp())) {
            catalogPage.clickClearFilters();
        } else if (request.getFollowUp() != null) {
            throw new IllegalArgumentException("Unknown catalog follow-up: " + request.getFollowUp());
        }

        return success(catalogPage.read());
    }

    /**
     * Product pages are public: a visitor views them without signing in, unless a user was explicitly requested.
     * The visitor's cart line, if any, is added first, from that product's page, in the same session.
     */
    @Override
    public Result<ViewProductResponse, SystemError> viewProduct(ViewProductRequest request) {
        var visitor = anonymous || requestedUser == null;
        if (visitor) {
            client.startVisitorSession();
        } else {
            runAs(TestUser.CUSTOMER);
        }
        resetPages();

        if (request.getCartSku() != null) {
            var cartProductPage = client.openProductPage(request.getCartSku());
            if (cartProductPage == null) {
                return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
            }
            cartProductPage.waitForLoaded();
            cartProductPage.inputQuantity(request.getCartQuantity());
            cartProductPage.clickAddToCart();
        }

        ProductPage productPage;
        if (Boolean.TRUE.equals(request.getFromCatalog())) {
            var catalogPage = client.openCatalogInSession();
            if (catalogPage == null) {
                return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
            }
            catalogPage.waitForLoaded();
            productPage = catalogPage.clickProduct(request.getSku());
        } else {
            productPage = client.openProductPage(request.getSku());
            if (productPage == null) {
                return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
            }
        }
        productPage.waitForLoaded();

        if (request.getQuantity() != null) {
            productPage.inputQuantity(request.getQuantity());
        }
        if (ViewProductRequest.INCREASE.equals(request.getQuantityStep())) {
            productPage.clickIncreaseQuantity();
        } else if (ViewProductRequest.DECREASE.equals(request.getQuantityStep())) {
            productPage.clickDecreaseQuantity();
        } else if (request.getQuantityStep() != null) {
            throw new IllegalArgumentException("Unknown quantity step: " + request.getQuantityStep());
        }
        var addToCart = Boolean.TRUE.equals(request.getAddToCart());
        if (addToCart) {
            productPage.clickAddToCart();
        }

        var response = productPage.read();

        if (ViewProductRequest.BACK_TO_CATALOG.equals(request.getFollowUp())) {
            var catalog = productPage.clickBackToCatalog().read();
            response.setCatalogSkus(catalog.getProducts().stream().map(product -> product.getSku()).toList());
        } else if (request.getFollowUp() != null) {
            throw new IllegalArgumentException("Unknown product follow-up: " + request.getFollowUp());
        }
        if (addToCart) {
            response.setCartLines(client.openCartPage().readLines());
        }

        return success(response);
    }

    /**
     * The cart is public: a visitor keeps one without signing in, unless a user was explicitly requested. A new
     * session first gets the cart lines, each from its product page; then the action runs, and the cart is read.
     */
    @Override
    public Result<ManageCartResponse, SystemError> manageCart(ManageCartRequest request) {
        if (!Boolean.TRUE.equals(request.getContinueSession())) {
            var filled = startCartSession(request.getCartLines());
            if (filled.isFailure()) {
                return failure(filled.getError());
            }
        }
        var action = request.getAction();
        if (action == null) {
            return success(ManageCartResponse.builder().build());
        }

        String message = null;
        Integer cartItemCount = null;
        CartPage cartPage = null;
        switch (action) {
            case ManageCartRequest.VIEW_CART -> {
                // The cart is read below.
            }
            case ManageCartRequest.BROWSE_CATALOG, ManageCartRequest.ADD_FROM_CATALOG -> {
                var catalogPage = client.openCatalogInSession();
                if (catalogPage == null) {
                    return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
                }
                catalogPage.waitForLoaded();
                if (ManageCartRequest.ADD_FROM_CATALOG.equals(action)) {
                    message = catalogPage.clickAddToCart(request.getSku());
                }
                cartItemCount = catalogPage.readCartItemCount();
            }
            case ManageCartRequest.ADD_FROM_PRODUCT_PAGE -> {
                var productPage = client.openProductPage(request.getSku());
                if (productPage == null) {
                    return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
                }
                productPage.waitForLoaded();
                productPage.inputQuantity(request.getQuantity());
                productPage.clickAddToCart();
                var shown = productPage.read();
                message = shown.getMessage();
                cartItemCount = shown.getCartItemCount();
            }
            case ManageCartRequest.CHANGE_QUANTITY, ManageCartRequest.INCREASE_QUANTITY,
                 ManageCartRequest.DECREASE_QUANTITY, ManageCartRequest.REMOVE_LINE -> {
                cartPage = client.openCartPage();
                cartPage.waitForLoaded();
                switch (action) {
                    case ManageCartRequest.CHANGE_QUANTITY -> cartPage.inputQuantity(request.getSku(), request.getQuantity());
                    case ManageCartRequest.INCREASE_QUANTITY -> cartPage.clickIncreaseQuantity(request.getSku());
                    case ManageCartRequest.DECREASE_QUANTITY -> cartPage.clickDecreaseQuantity(request.getSku());
                    default -> cartPage.clickRemove(request.getSku());
                }
                message = cartPage.readNotification();
            }
            case ManageCartRequest.REVISIT, ManageCartRequest.SIGN_IN -> {
                if (ManageCartRequest.REVISIT.equals(action)) {
                    client.revisit();
                }
                var catalogPage = client.openCatalogInSession();
                if (catalogPage == null) {
                    return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
                }
                catalogPage.waitForLoaded();
                if (ManageCartRequest.SIGN_IN.equals(action)) {
                    client.signIn(TestUser.CUSTOMER);
                }
                cartItemCount = client.currentPageInSession().readCartItemCount();
            }
            default -> throw new IllegalArgumentException("Unknown cart action: " + action);
        }

        if (cartPage == null) {
            cartPage = client.openCartPage();
        }
        var response = cartPage.read(message, cartItemCount);
        response.setAskedToSignIn(client.isOnLoginPage());
        return success(response);
    }

    /**
     * Checkout is for signed-in customers: a visitor is asked to sign in, an admin is refused. A new session first
     * gets the cart lines, each from its product page; then the checkout is opened, and, when asked, the order placed.
     */
    @Override
    public Result<CheckOutResponse, SystemError> checkOut(CheckOutRequest request) {
        if (!Boolean.TRUE.equals(request.getContinueSession())) {
            var lines = request.getCartLines() == null ? List.<ManageCartRequest.CartLineDto>of() : request.getCartLines().stream()
                    .map(line -> ManageCartRequest.CartLineDto.builder().sku(line.getSku()).quantity(line.getQuantity()).build())
                    .toList();
            var filled = startCartSession(lines);
            if (filled.isFailure()) {
                return failure(filled.getError());
            }
        }

        if (CheckOutRequest.PROCEED_FROM_CART.equals(request.getEntry())) {
            client.proceedToCheckoutFromCart();
        } else if (request.getEntry() == null || CheckOutRequest.OPEN.equals(request.getEntry())) {
            client.openCheckoutPage();
        } else {
            throw new IllegalArgumentException("Unknown checkout entry: " + request.getEntry());
        }

        if (client.isOnLoginPage()) {
            if (!Boolean.TRUE.equals(request.getSignIn())) {
                return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
            }
            client.signInOnLoginForm(TestUser.CUSTOMER);
        }
        if (client.isRefused()) {
            return failure(MyShopUiClient.PERMISSION_DENIED);
        }

        var pageClient = client.sessionPageClient();
        var checkoutPage = new CheckoutPage(pageClient);
        var response = CheckOutResponse.builder().messageDetails(List.of()).build();
        if (!checkoutPage.isShown()) {
            return success(readWhereTheCheckoutSentTheCustomer(response));
        }

        response.setOpenedTaxCountry(checkoutPage.readTaxCountry());
        response.setOpenedEstimatedTaxRate(checkoutPage.readEstimatedTaxRate());
        if (request.getShippingAddress() != null) {
            checkoutPage.inputShippingAddress(request.getShippingAddress());
        }
        if (Boolean.FALSE.equals(request.getSaveAsDefault())) {
            checkoutPage.setSaveAsDefault(false);
        }
        if (request.getCouponCode() != null) {
            checkoutPage.inputCouponCode(request.getCouponCode());
        }
        if (!Boolean.TRUE.equals(request.getPlaceOrder())) {
            response.setShownPage(CheckOutResponse.CHECKOUT);
            checkoutPage.readInto(response);
            return success(response);
        }

        checkoutPage.clickPlaceOrder();
        var confirmationPage = new CheckoutConfirmationPage(pageClient);
        if (confirmationPage.isShown()) {
            response.setShownPage(CheckOutResponse.CONFIRMATION);
            confirmationPage.readInto(response);
        } else {
            response.setShownPage(CheckOutResponse.CHECKOUT);
            checkoutPage.readInto(response);
            response.setConfirmedOrders(List.of());
        }
        response.setCartLines(client.openCartPage().readLines());
        return success(response);
    }

    /** The checkout was not shown: the app sent the customer to the cart, or to some other page. */
    private CheckOutResponse readWhereTheCheckoutSentTheCustomer(CheckOutResponse response) {
        var path = client.currentPath();
        if (path.endsWith("/cart")) {
            var cart = new CartPage(client.sessionPageClient()).read(null, null);
            response.setShownPage(CheckOutResponse.CART);
            response.setMessage(cart.getMessage());
        } else {
            response.setShownPage(path);
        }
        return response;
    }

    /** A fresh session (the visitor's, or the requested user's) whose cart gets these lines, in order. */
    private Result<Void, SystemError> startCartSession(List<ManageCartRequest.CartLineDto> cartLines) {
        if (anonymous || requestedUser == null) {
            client.startVisitorSession();
        } else {
            runAs(TestUser.CUSTOMER);
        }
        resetPages();
        for (var line : cartLines == null ? List.<ManageCartRequest.CartLineDto>of() : cartLines) {
            var productPage = client.openProductPage(line.getSku());
            if (productPage == null) {
                return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
            }
            productPage.waitForLoaded();
            productPage.inputQuantity(line.getQuantity());
            productPage.clickAddToCart();
        }
        return success();
    }

    // --- unsupported inputs ---

    private static UnsupportedOperationException unsupportedOperation(String operation) {
        return new UnsupportedOperationException(operation + " is not supported by the UI channel yet; run this scenario on the API channel only");
    }

    /**
     * The UI has no control for this input (no idempotency key, no paging controls, no shipping address, no
     * customer filter yet): API-channel scenarios only.
     */
    private static void requireNotSet(String input, Object value) {
        if (value != null) {
            throw new UnsupportedOperationException("The UI channel cannot send '" + input + "'; run this scenario on the API channel only");
        }
    }

    // --- identity ---

    /** An explicitly requested identity wins; otherwise the operation runs as its default user. */
    private void runAs(TestUser operationDefault) {
        var user = requestedUser != null ? requestedUser : operationDefault;
        if (client.switchUser(user)) {
            resetPages();
        }
    }

    /** The browser session was replaced: no page is open any more. */
    private void resetPages() {
        currentPage = Page.NONE;
        homePage = null;
        newOrderPage = null;
        orderHistoryPage = null;
        orderDetailsPage = null;
        couponManagementPage = null;
    }

    private <T> Result<T, SystemError> loginRequired(String path) {
        client.assertLoginRequiredFor(path);
        return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
    }

    private <T> Result<T, SystemError> loginRequiredForOrder(String orderNumber) {
        var encodedOrderNumber = URLEncoder.encode(orderNumber, StandardCharsets.UTF_8).replace("+", "%20");
        client.assertLoginRequiredFor("/order-details?orderNumber=" + encodedOrderNumber, "/order-details/" + encodedOrderNumber);
        return failure(MyShopUiClient.AUTHENTICATION_REQUIRED);
    }

    // --- page navigation ---

    private HomePage getHomePage() {
        if (homePage == null || currentPage != Page.HOME) {
            homePage = client.openHomePage();
            currentPage = Page.HOME;
        }
        return homePage;
    }

    /** A user who has no link to the page opens its URL, which is what can be refused. */
    private Result<Void, SystemError> ensureOnNewOrderPage() {
        if (currentPage == Page.NEW_ORDER) {
            return success();
        }

        var homePage = getHomePage();
        if (requestedUser != TestUser.ADMIN) {
            newOrderPage = homePage.clickNewOrder();
            currentPage = Page.NEW_ORDER;
            return success();
        }

        var result = client.openNewOrderPage();
        if (result.isFailure()) {
            currentPage = Page.NONE;
            return failure(result.getError());
        }

        newOrderPage = result.getValue();
        currentPage = Page.NEW_ORDER;
        return success();
    }

    private Result<Void, SystemError> ensureOnCouponManagementPage() {
        return currentPage == Page.COUPON_MANAGEMENT ? success() : navigateToCouponManagementPage();
    }

    /** A user who has no link to the page opens its URL, which is what can be refused. */
    private Result<Void, SystemError> navigateToCouponManagementPage() {
        var homePage = getHomePage();
        if (requestedUser == null || requestedUser == TestUser.ADMIN) {
            couponManagementPage = homePage.clickCouponManagement();
            currentPage = Page.COUPON_MANAGEMENT;
            return success();
        }

        var result = client.openCouponManagementPage();
        if (result.isFailure()) {
            currentPage = Page.NONE;
            return failure(result.getError());
        }

        couponManagementPage = result.getValue();
        currentPage = Page.COUPON_MANAGEMENT;
        return success();
    }

    private Result<Void, SystemError> ensureOnOrderDetailsPage(String orderNumber) {
        getHomePage();
        orderDetailsPage = client.openOrderDetailsPage(orderNumber);
        currentPage = Page.ORDER_DETAILS;

        return orderDetailsPage.getLoadResult(orderNumber);
    }

    private enum Page {
        NONE,
        HOME,
        NEW_ORDER,
        ORDER_HISTORY,
        ORDER_DETAILS,
        COUPON_MANAGEMENT
    }
}
