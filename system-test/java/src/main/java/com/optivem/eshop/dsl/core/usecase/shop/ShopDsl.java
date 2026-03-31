package com.optivem.eshop.dsl.core.usecase.shop;

import com.optivem.eshop.dsl.driver.port.shop.ShopDriver;
import com.optivem.eshop.dsl.core.usecase.shop.usecases.GoToShop;
import com.optivem.eshop.dsl.core.usecase.shop.usecases.PlaceOrder;
import com.optivem.eshop.dsl.core.usecase.shop.usecases.ViewOrder;
import com.optivem.eshop.dsl.common.Closer;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;

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

    public ViewOrder viewOrder() {
        return new ViewOrder(driver, context);
    }

}



