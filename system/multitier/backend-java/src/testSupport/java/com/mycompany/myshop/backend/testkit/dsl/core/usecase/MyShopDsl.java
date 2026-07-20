package com.mycompany.myshop.backend.testkit.dsl.core.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.BrowseCoupons;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.BrowseOrderHistory;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.GoToMyShop;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.PlaceOrder;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.PublishCoupon;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.ViewOrder;

/**
 * The system under test, one class per use case. Every call produces an outcome the caller states an
 * expectation on ({@code .execute().shouldSucceed()} / {@code .shouldFail()}) — unlike the external
 * stub DSLs, whose {@code execute()} is fire-and-forget because programming an in-process WireMock
 * cannot fail.
 */
public class MyShopDsl {

    private final MyShopDriver driver;
    private final ObjectMapper objectMapper;

    public MyShopDsl(MyShopDriver driver, ObjectMapper objectMapper) {
        this.driver = driver;
        this.objectMapper = objectMapper;
    }

    public GoToMyShop goToMyShop() {
        return new GoToMyShop(driver);
    }

    public PlaceOrder placeOrder() {
        return new PlaceOrder(driver, objectMapper);
    }

    public ViewOrder viewOrder() {
        return new ViewOrder(driver, objectMapper);
    }

    public BrowseOrderHistory browseOrderHistory() {
        return new BrowseOrderHistory(driver);
    }

    public PublishCoupon publishCoupon() {
        return new PublishCoupon(driver);
    }

    public BrowseCoupons browseCoupons() {
        return new BrowseCoupons(driver);
    }
}
