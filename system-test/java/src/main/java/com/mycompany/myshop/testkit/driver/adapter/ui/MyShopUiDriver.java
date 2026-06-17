package com.mycompany.myshop.testkit.driver.adapter.ui;

import com.microsoft.playwright.Browser;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.MyShopUiClient;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.CouponManagementPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.HomePage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.NewOrderPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderDetailsPage;
import com.mycompany.myshop.testkit.driver.adapter.ui.client.pages.OrderHistoryPage;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.PlaceOrderResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.PublishCouponRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewOrderResponse;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.common.Result;

import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.failure;
import static com.mycompany.myshop.testkit.dsl.core.usecase.commons.SystemResults.success;

public class MyShopUiDriver implements MyShopDriver {
    private final MyShopUiClient client;

    private Page currentPage = Page.NONE;
    private HomePage homePage;
    private NewOrderPage newOrderPage;
    private OrderHistoryPage orderHistoryPage;
    private OrderDetailsPage orderDetailsPage;
    private CouponManagementPage couponManagementPage;

    public MyShopUiDriver(String baseUrl, Browser browser) {
        this.client = new MyShopUiClient(baseUrl, browser);
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public Result<Void, SystemError> goToMyShop() {
        homePage = client.openHomePage();

        if (!client.isStatusOk() || !client.isPageLoaded()) {
            return failure("Failed to load home page");
        }

        currentPage = Page.HOME;
        return success();
    }

    @Override
    public Result<PlaceOrderResponse, SystemError> placeOrder(PlaceOrderRequest request) {
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
    public Result<Void, SystemError> cancelOrder(String orderNumber) {
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
    public Result<Void, SystemError> deliverOrder(String orderNumber) {
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
    public Result<ViewOrderResponse, SystemError> viewOrder(String orderNumber) {
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
        var shippingFee = orderDetailsPage.getShippingFee();
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
                .shippingFee(shippingFee)
                .totalPrice(totalPrice)
                .status(status)
                .appliedCouponCode(appliedCoupon)
                .build();

        return success(response);
    }

    @Override
    public Result<Void, SystemError> publishCoupon(PublishCouponRequest request) {
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
    public Result<BrowseCouponsResponse, SystemError> browseCoupons() {
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

    private Result<Void, SystemError> ensureOnOrderDetailsPage(String orderNumber) {
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
