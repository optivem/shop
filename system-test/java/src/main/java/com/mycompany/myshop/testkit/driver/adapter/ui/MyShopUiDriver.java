package com.mycompany.myshop.testkit.driver.adapter.ui;

import com.microsoft.playwright.Browser;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.MyShopUiClient;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CatalogPage;
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
            case CUSTOMER -> TestUser.customer(identity.customerIndex());
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
