package com.optivem.eshop.dsl.core.usecase.shop.usecases.base;

import com.optivem.eshop.dsl.driver.port.shop.ShopDriver;
import com.optivem.eshop.dsl.core.shared.BaseUseCase;
import com.optivem.eshop.dsl.core.shared.UseCaseContext;

public abstract class BaseShopUseCase<TResponse, TVerification> extends BaseUseCase<ShopDriver, TResponse, TVerification> {
    protected BaseShopUseCase(ShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }
}



