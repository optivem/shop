package com.mycompany.myshop.testkit.driver.adapter.api;

import com.mycompany.myshop.testkit.driver.adapter.api.client.ApiIdentity;
import com.mycompany.myshop.testkit.driver.adapter.api.client.MyShopApiClient;
import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCatalogResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCustomersRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCustomersResponse;
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
import com.mycompany.myshop.testkit.driver.port.dtos.SignUpRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.SignUpResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.UpdateProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProfileResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ViewProductResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.ManageCartResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckOutResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckAccessibilityRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.CheckAccessibilityResponse;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.adapter.shared.client.http.TestUser;
import com.mycompany.myshop.testkit.driver.port.UserIdentity;
import com.mycompany.myshop.testkit.common.Closer;
import com.mycompany.myshop.testkit.common.Result;

import java.util.function.Supplier;

public class MyShopApiDriver implements MyShopDriver {
    private final MyShopApiClient apiClient;

    private final boolean signsIn;

    private TestUser requestedUser;
    private boolean anonymous;
    private Supplier<UserIdentity> scenarioAdmin = () -> {
        throw new IllegalStateException("No admin for this admin-only operation: act as an admin, or give the driver"
                + " the scenario's admin (useScenarioAdmin)");
    };

    public MyShopApiDriver(String baseUrl) {
        this(baseUrl, null);
    }

    public MyShopApiDriver(String baseUrl, String keycloakBaseUrl) {
        this.apiClient = new MyShopApiClient(baseUrl, keycloakBaseUrl);
        this.signsIn = keycloakBaseUrl != null && !keycloakBaseUrl.isBlank();
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
            case CUSTOMER, ADMIN -> TestUser.of(identity);
        };
    }

    @Override
    public void useScenarioAdmin(Supplier<UserIdentity> admin) {
        this.scenarioAdmin = admin;
    }

    /** An explicitly requested identity wins; otherwise the operation runs as its default user. */
    private void runAs(TestUser operationDefault) {
        var user = requestedUser != null ? requestedUser : operationDefault;
        if (anonymous) {
            apiClient.as(ApiIdentity.ANONYMOUS);
        } else {
            apiClient.asUser(user);
        }
    }

    /**
     * Runs an admin-only operation: as the requested identity, else as the scenario's admin. Without sign-in (no
     * Keycloak) no token is sent anyway, so no admin is created.
     */
    private void runAsAdmin() {
        var needsAdmin = requestedUser == null && !anonymous && signsIn;
        runAs(needsAdmin ? TestUser.of(scenarioAdmin.get()) : TestUser.CUSTOMER);
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
        runAsAdmin();
        return apiClient.orders().deliverOrder(request.getOrderNumber())
                .mapError(SystemErrorMapper::from)
                .map(value -> DeliverOrderResponse.builder().build());
    }

    @Override
    public Result<ViewOrderResponse, SystemError> viewOrder(ViewOrderRequest request) {
        requireNotSet("whenAskedToSignIn", request.getWhenAskedToSignIn());
        runAs(TestUser.CUSTOMER);
        return apiClient.orders().viewOrder(request.getOrderNumber()).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<BrowseOrderHistoryResponse, SystemError> browseOrderHistory(BrowseOrderHistoryRequest request) {
        requireNotSet("followUp", request.getFollowUp());
        runAs(TestUser.CUSTOMER);
        return apiClient.orders().browseOrderHistory(request.getOrderNumber(), request.getCustomer(), request.getPage(), request.getSize()).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<BrowseCustomersResponse, SystemError> browseCustomers(BrowseCustomersRequest request) {
        runAsAdmin();
        return apiClient.admin().browseCustomers().mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<PublishCouponResponse, SystemError> publishCoupon(PublishCouponRequest request) {
        runAsAdmin();
        return apiClient.coupons().publishCoupon(request)
                .mapError(SystemErrorMapper::from)
                .map(value -> PublishCouponResponse.builder().build());
    }

    @Override
    public Result<BrowseCouponsResponse, SystemError> browseCoupons(BrowseCouponsRequest request) {
        runAsAdmin();
        return apiClient.coupons().browseCoupons(request.getPage(), request.getSize()).mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<ViewProfileResponse, SystemError> viewProfile(ViewProfileRequest request) {
        requireNotSet("whenAskedToSignIn", request.getWhenAskedToSignIn());
        runAs(TestUser.CUSTOMER);
        return apiClient.profile().viewProfile().mapError(SystemErrorMapper::from);
    }

    @Override
    public Result<ViewProfileResponse, SystemError> updateProfile(UpdateProfileRequest request) {
        runAs(TestUser.CUSTOMER);
        return apiClient.profile().updateProfile(request).mapError(SystemErrorMapper::from);
    }

    /** The catalog is public: a visitor browses it without signing in, unless a user was explicitly requested. */
    @Override
    public Result<BrowseCatalogResponse, SystemError> browseCatalog(BrowseCatalogRequest request) {
        requireNotSet("headerCategory", request.getHeaderCategory());
        requireNotSet("search", request.getSearch());
        requireNotSet("category", request.getCategory());
        requireNotSet("brands", request.getBrands());
        requireNotSet("sort", request.getSort());
        requireNotSet("followUp", request.getFollowUp());

        if (anonymous || requestedUser == null) {
            apiClient.as(ApiIdentity.ANONYMOUS);
        } else {
            runAs(TestUser.CUSTOMER);
        }
        return apiClient.products().browseProducts()
                .mapError(SystemErrorMapper::from)
                .map(products -> BrowseCatalogResponse.builder()
                        .products(products)
                        .productCount(products.size())
                        .build());
    }

    @Override
    public Result<ViewProductResponse, SystemError> viewProduct(ViewProductRequest request) {
        requireNotSet("fromCatalog", request.getFromCatalog());
        requireNotSet("cartSku", request.getCartSku());
        requireNotSet("quantity", request.getQuantity());
        requireNotSet("quantityStep", request.getQuantityStep());
        requireNotSet("addToCart", request.getAddToCart());
        requireNotSet("followUp", request.getFollowUp());

        if (anonymous || requestedUser == null) {
            apiClient.as(ApiIdentity.ANONYMOUS);
        } else {
            runAs(TestUser.CUSTOMER);
        }
        return apiClient.products().viewProduct(request.getSku())
                .mapError(SystemErrorMapper::from)
                .map(product -> ViewProductResponse.builder()
                        .sku(product.getSku())
                        .title(product.getTitle())
                        .brand(product.getBrand())
                        .category(product.getCategory())
                        .description(product.getDescription())
                        .price(product.getPrice())
                        .build());
    }

    /** The cart lives in the browser (session, cookie or local storage): there is no cart API. */
    @Override
    public Result<ManageCartResponse, SystemError> manageCart(ManageCartRequest request) {
        throw new UnsupportedOperationException("The API channel has no cart; run this scenario on the UI channel only");
    }

    @Override
    public Result<CheckOutResponse, SystemError> checkOut(CheckOutRequest request) {
        throw new UnsupportedOperationException("The API channel has no cart to check out; run this scenario on the UI channel only");
    }

    /** Signing up is the identity provider's registration form, reached from the shop's pages: there is no sign-up API. */
    @Override
    public Result<SignUpResponse, SystemError> signUp(SignUpRequest request) {
        throw new UnsupportedOperationException("The API channel has no sign-up form; run this scenario on the UI channel only");
    }

    /** The API serves data, not pages: accessibility is a UI-channel check. */
    @Override
    public Result<CheckAccessibilityResponse, SystemError> checkAccessibility(CheckAccessibilityRequest request) {
        throw new UnsupportedOperationException("The API channel has no pages to check for accessibility; run this scenario on the UI channel only");
    }

    /** The API serves the unfiltered catalog; filtering, search and sort happen in the UI. */
    private static void requireNotSet(String input, Object value) {
        if (value != null) {
            throw new UnsupportedOperationException("The API channel cannot send '" + input + "'; run this scenario on the UI channel only");
        }
    }
}
