package com.optivem.shop.testkit.driver.adapter.shop.ui;

import com.microsoft.playwright.Browser;
import com.optivem.shop.testkit.driver.adapter.shop.ui.client.ShopUiClient;
import com.optivem.shop.testkit.driver.adapter.shop.ui.client.pages.CouponManagementPage;
import com.optivem.shop.testkit.driver.adapter.shop.ui.client.pages.HomePage;
import com.optivem.shop.testkit.driver.adapter.shop.ui.client.pages.NewOrderPage;
import com.optivem.shop.testkit.driver.adapter.shop.ui.client.pages.OrderDetailsPage;
import com.optivem.shop.testkit.driver.adapter.shop.ui.client.pages.OrderHistoryPage;
import com.optivem.shop.testkit.driver.port.shop.dtos.BrowseCouponsResponse;
import com.optivem.shop.testkit.driver.port.shop.dtos.PlaceOrderRequest;
import com.optivem.shop.testkit.driver.port.shop.dtos.PlaceOrderResponse;
import com.optivem.shop.testkit.driver.port.shop.dtos.PublishCouponRequest;
import com.optivem.shop.testkit.driver.port.shop.dtos.ViewOrderResponse;
import com.optivem.shop.testkit.driver.port.shop.ShopDriver;
import com.optivem.shop.testkit.driver.port.shared.dtos.ErrorResponse;
import com.optivem.shop.testkit.common.Result;

import static com.optivem.shop.testkit.dsl.core.usecase.shop.commons.SystemResults.failure;
import static com.optivem.shop.testkit.dsl.core.usecase.shop.commons.SystemResults.success;

public class ShopUiDriver implements ShopDriver {
    private final ShopUiClient client;

    private Page currentPage = Page.NONE;
    private HomePage homePage;
    private NewOrderPage newOrderPage;
    private OrderHistoryPage orderHistoryPage;
    private OrderDetailsPage orderDetailsPage;
    private CouponManagementPage couponManagementPage;

    public ShopUiDriver(String baseUrl, Browser browser) {
        this.client = new ShopUiClient(baseUrl, browser);
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public Result<Void, ErrorResponse> goToShop() {
        homePage = client.openHomePage();

        if (!client.isStatusOk() || !client.isPageLoaded()) {
            return failure("Failed to load home page");
        }

        currentPage = Page.HOME;
        return success();
    }

    @Override
    public Result<PlaceOrderResponse, ErrorResponse> placeOrder(PlaceOrderRequest request) {
        var sku = request.getSku();
        var quantity = request.getQuantity();
        var country = request.getCountry();
        var couponCode = request.getCouponCode();

        ensureOnNewOrderPage();
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
    public Result<Void, ErrorResponse> cancelOrder(String orderNumber) {
        var viewResult = viewOrder(orderNumber);

        if (viewResult.isFailure()) {
            return viewResult.mapVoid();
        }

        orderDetailsPage.clickCancelOrder();

        var result = orderDetailsPage.getResult();

        if (result.isFailure()) {
            return result.mapVoid();
        }

        return success();
    }

    @Override
    public Result<Void, ErrorResponse> deliverOrder(String orderNumber) {
        var viewResult = viewOrder(orderNumber);

        if (viewResult.isFailure()) {
            return viewResult.mapVoid();
        }

        orderDetailsPage.clickDeliverOrder();

        var result = orderDetailsPage.getResult();

        if (result.isFailure()) {
            return result.mapVoid();
        }

        return success();
    }

    @Override
    public Result<ViewOrderResponse, ErrorResponse> viewOrder(String orderNumber) {
        var result = ensureOnOrderDetailsPage(orderNumber);
        if (result.isFailure()) {
            return failure(result.getError());
        }

        var isSuccess = orderDetailsPage.isLoadedSuccessfully();

        if (!isSuccess) {
            return Result.failure(result.getError());
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
    public Result<Void, ErrorResponse> publishCoupon(PublishCouponRequest request) {
        ensureOnCouponManagementPage();

        couponManagementPage.inputCouponCode(request.getCode());
        couponManagementPage.inputDiscountRate(request.getDiscountRate());
        couponManagementPage.inputValidFrom(request.getValidFrom());
        couponManagementPage.inputValidTo(request.getValidTo());
        couponManagementPage.inputUsageLimit(request.getUsageLimit());
        couponManagementPage.clickPublishCoupon();

        return couponManagementPage.getResult().mapVoid();
    }

    @Override
    public Result<BrowseCouponsResponse, ErrorResponse> browseCoupons() {
        navigateToCouponManagementPage();

        var coupons = couponManagementPage.readCoupons();

        var response = BrowseCouponsResponse.builder()
                .coupons(coupons)
                .build();

        return success(response);
    }

    // --- page navigation ---

    private HomePage getHomePage() {
        if (homePage == null || currentPage != Page.HOME) {
            homePage = client.openHomePage();
            currentPage = Page.HOME;
        }
        return homePage;
    }

    private void ensureOnNewOrderPage() {
        if (currentPage != Page.NEW_ORDER) {
            newOrderPage = getHomePage().clickNewOrder();
            currentPage = Page.NEW_ORDER;
        }
    }

    private void ensureOnCouponManagementPage() {
        if (currentPage != Page.COUPON_MANAGEMENT) {
            navigateToCouponManagementPage();
        }
    }

    private void navigateToCouponManagementPage() {
        couponManagementPage = getHomePage().clickCouponManagement();
        currentPage = Page.COUPON_MANAGEMENT;
    }

    private void ensureOnOrderHistoryPage() {
        if (currentPage != Page.ORDER_HISTORY) {
            orderHistoryPage = getHomePage().clickOrderHistory();
            currentPage = Page.ORDER_HISTORY;
        }
    }

    private Result<Void, ErrorResponse> ensureOnOrderDetailsPage(String orderNumber) {
        ensureOnOrderHistoryPage();
        orderHistoryPage.inputOrderNumber(orderNumber);
        orderHistoryPage.clickSearch();

        var isOrderListed = orderHistoryPage.isOrderListed(orderNumber);
        if (!isOrderListed) {
            return failure("Order " + orderNumber + " does not exist.");
        }

        orderDetailsPage = orderHistoryPage.clickViewOrderDetails(orderNumber);
        currentPage = Page.ORDER_DETAILS;

        return success();
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
