package com.mycompany.myshop.testkit.driver.port;

import com.mycompany.myshop.testkit.driver.port.dtos.error.SystemError;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;
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
import com.mycompany.myshop.testkit.common.Result;

public interface MyShopDriver extends AutoCloseable {
    /** Selects who subsequent operations are performed as. */
    void actAs(UserIdentity identity);

    Result<GoToMyShopResponse, SystemError> goToMyShop(GoToMyShopRequest request);

    Result<PlaceOrderResponse, SystemError> placeOrder(PlaceOrderRequest request);

    Result<CancelOrderResponse, SystemError> cancelOrder(CancelOrderRequest request);

    Result<DeliverOrderResponse, SystemError> deliverOrder(DeliverOrderRequest request);

    Result<ViewOrderResponse, SystemError> viewOrder(ViewOrderRequest request);

    Result<BrowseOrderHistoryResponse, SystemError> browseOrderHistory(BrowseOrderHistoryRequest request);

    Result<PublishCouponResponse, SystemError> publishCoupon(PublishCouponRequest request);

    Result<BrowseCouponsResponse, SystemError> browseCoupons(BrowseCouponsRequest request);
}
