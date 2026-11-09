package com.mycompany.myshop.testkit.driver.adapter.ui;

import com.microsoft.playwright.Browser;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.MyShopUiClient;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CouponManagementPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.HomePage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.NewOrderPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderDetailsPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderHistoryPage;
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
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.common.Result;
import com.mycompany.myshop.testkit.common.domain.OrderStatus;

import java.net.URLEncoder;
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
            return loginRequired("/order-details?orderNumber=" + URLEncoder.encode(orderNumber, StandardCharsets.UTF_8).replace("+", "%20"));
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
            currentPage = Page.NONE;
            homePage = null;
            newOrderPage = null;
            orderHistoryPage = null;
            orderDetailsPage = null;
            couponManagementPage = null;
        }
    }

    private <T> Result<T, SystemError> loginRequired(String path) {
        client.assertLoginRequiredFor(path);
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
