package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import com.mycompany.myshop.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsRequest;
import com.mycompany.myshop.testkit.driver.port.dtos.BrowseCouponsResponse;

public class BrowseCoupons extends BaseMyShopUseCase<BrowseCouponsResponse, BrowseCouponsVerification> {
    private String page;
    private String size;

    public BrowseCoupons(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public BrowseCoupons page(String page) {
        this.page = page;
        return this;
    }

    public BrowseCoupons size(String size) {
        this.size = size;
        return this;
    }

    @Override
    public UseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification> execute() {
        var request = BrowseCouponsRequest.builder().page(page).size(size).build();
        var result = driver.browseCoupons(request);
        return new UseCaseResult<>(result, context, BrowseCouponsVerification::new);
    }
}
