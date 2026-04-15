package com.optivem.shop.testkit.dsl.core.usecase.shop.usecases;

import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.base.BaseShopUseCase;
import com.optivem.shop.testkit.driver.port.shop.ShopDriver;
import com.optivem.shop.testkit.driver.port.shop.dtos.BrowseCouponsResponse;

public class BrowseCoupons extends BaseShopUseCase<BrowseCouponsResponse, BrowseCouponsVerification> {
    public BrowseCoupons(ShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification> execute() {
        var result = driver.browseCoupons();
        return new UseCaseResult<>(result, context, BrowseCouponsVerification::new);
    }
}
