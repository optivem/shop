package com.mycompany.myshop.testkit.driver.adapter.api;

import com.mycompany.myshop.testkit.driver.adapter.api.client.ApiIdentity;
import com.mycompany.myshop.testkit.driver.adapter.api.client.MyShopApiClient;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
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
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.common.Result;

public class MyShopApiDriver implements MyShopDriver {
    private final MyShopApiClient apiClient;

    private TestUser requestedUser;
    private boolean anonymous;

    public MyShopApiDriver(String baseUrl) {
        this(baseUrl, null);
    }

    public MyShopApiDriver(String baseUrl, String keycloakBaseUrl) {
        this.apiClient = new MyShopApiClient(baseUrl, keycloakBaseUrl);
    }

    @Override
    public void close() {
        Closer.close(apiClient);
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

    /** An explicitly requested identity wins; otherwise the operation runs as its default user. */
    private void runAs(TestUser operationDefault) {
        var user = requestedUser != null ? requestedUser : operationDefault;
        if (anonymous) {
            apiClient.as(ApiIdentity.ANONYMOUS);
        } else if (user == TestUser.ADMIN) {
            apiClient.as(ApiIdentity.ADMIN);
        } else {
            apiClient.asCustomer(user);
        }
    }

    @Override
    public Result<GoToMyShopResponse, SystemError> goToMyShop(GoToMyShopRequest request) {
        runAs(TestUser.CUSTOMER);
        return apiClient.health().checkHealth()
                .mapError(SystemErrorMapper::from)
                .map(value -> GoToMyShopResponse.builder().build());
    }

    @Override
    public Result<PlaceOrderResponse, SystemError> placeOrder(PlaceOrderRequest request) {
        runAs(TestUser.CUSTOMER);
        return apiClient.orders().placeOrder(request).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<CancelOrderResponse, SystemError> cancelOrder(CancelOrderRequest request) {
        runAs(TestUser.CUSTOMER);
        return apiClient.orders().cancelOrder(request.getOrderNumber())
                .mapError(SystemErrorMapper::from)
                .map(value -> CancelOrderResponse.builder().build());
    }

    @Override
    public Result<DeliverOrderResponse, SystemError> deliverOrder(DeliverOrderRequest request) {
        runAs(TestUser.ADMIN);
        return apiClient.orders().deliverOrder(request.getOrderNumber())
                .mapError(SystemErrorMapper::from)
                .map(value -> DeliverOrderResponse.builder().build());
    }

    @Override
    public Result<ViewOrderResponse, SystemError> viewOrder(ViewOrderRequest request) {
        runAs(TestUser.CUSTOMER);
        return apiClient.orders().viewOrder(request.getOrderNumber()).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<BrowseOrderHistoryResponse, SystemError> browseOrderHistory(BrowseOrderHistoryRequest request) {
        runAs(TestUser.CUSTOMER);
        return apiClient.orders().browseOrderHistory(request.getOrderNumber(), request.getPage(), request.getSize()).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<PublishCouponResponse, SystemError> publishCoupon(PublishCouponRequest request) {
        runAs(TestUser.ADMIN);
        return apiClient.coupons().publishCoupon(request)
                .mapError(SystemErrorMapper::from)
                .map(value -> PublishCouponResponse.builder().build());
    }

    @Override
    public Result<BrowseCouponsResponse, SystemError> browseCoupons(BrowseCouponsRequest request) {
        runAs(TestUser.ADMIN);
        return apiClient.coupons().browseCoupons(request.getPage(), request.getSize()).mapError(SystemErrorMapper::from);
    }
}
