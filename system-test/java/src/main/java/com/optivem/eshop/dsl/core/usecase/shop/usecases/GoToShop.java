package com.optivem.eshop.dsl.core.usecase.shop.usecases;

import com.optivem.eshop.dsl.driver.port.shop.ShopDriver;
import com.optivem.eshop.dsl.core.usecase.shop.usecases.base.BaseShopUseCase;
import com.optivem.eshop.dsl.core.shared.UseCaseResult;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;
import com.optivem.eshop.dsl.core.shared.VoidVerification;

public class GoToShop extends BaseShopUseCase<Void, VoidVerification> {
    public GoToShop(ShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var result = driver.goToShop();
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}



