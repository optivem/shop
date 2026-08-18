package com.mycompany.myshop.backend.testkit.dsl.core.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.BrowseCoupons;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.BrowseOrderHistory;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.CancelOrder;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.GoToMyShop;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.PlaceOrder;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.PublishCoupon;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.ViewOrder;

public class MyShopDsl {

    private final MyShopDriver driver;
    private final ObjectMapper objectMapper;

    private final UseCaseContext context = new UseCaseContext();

    public MyShopDsl(MyShopDriver driver, ObjectMapper objectMapper) {
        this.driver = driver;
        this.objectMapper = objectMapper;
    }

    public GoToMyShop goToMyShop() {
        return new GoToMyShop(driver, context);
    }

    public PlaceOrder placeOrder() {
        return new PlaceOrder(driver, context, objectMapper);
    }

    public ViewOrder viewOrder() {
        return new ViewOrder(driver, context, objectMapper);
    }

    public CancelOrder cancelOrder() {
        return new CancelOrder(driver, context, objectMapper);
    }

    public BrowseOrderHistory browseOrderHistory() {
        return new BrowseOrderHistory(driver, context);
    }

    public PublishCoupon publishCoupon() {
        return new PublishCoupon(driver, context);
    }

    public BrowseCoupons browseCoupons() {
        return new BrowseCoupons(driver, context);
    }
}
