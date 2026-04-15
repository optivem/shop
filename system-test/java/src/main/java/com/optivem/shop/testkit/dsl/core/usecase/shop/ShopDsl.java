package com.optivem.shop.testkit.dsl.core.usecase.shop;

import com.optivem.shop.testkit.driver.port.shop.ShopDriver;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.BrowseCoupons;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.CancelOrder;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.DeliverOrder;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.GoToShop;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.PlaceOrder;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.PublishCoupon;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.ViewOrder;
import com.optivem.shop.testkit.common.Closer;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;

import java.io.Closeable;

public class ShopDsl implements Closeable {
    private final ShopDriver driver;
    private final UseCaseContext context;

    public ShopDsl(ShopDriver driver, UseCaseContext context) {
        this.driver = driver;
        this.context = context;
    }

    @Override
    public void close() {
        Closer.close(driver);
    }

    public GoToShop goToShop() {
        return new GoToShop(driver, context);
    }

    public PlaceOrder placeOrder() {
        return new PlaceOrder(driver, context);
    }

    public CancelOrder cancelOrder() {
        return new CancelOrder(driver, context);
    }

    public DeliverOrder deliverOrder() {
        return new DeliverOrder(driver, context);
    }

    public ViewOrder viewOrder() {
        return new ViewOrder(driver, context);
    }

    public PublishCoupon publishCoupon() {
        return new PublishCoupon(driver, context);
    }

    public BrowseCoupons browseCoupons() {
        return new BrowseCoupons(driver, context);
    }
}
