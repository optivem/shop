package com.optivem.shop.testkit.dsl.core.usecase.shop.usecases;

import com.optivem.shop.testkit.driver.port.shop.ShopDriver;
import com.optivem.shop.testkit.dsl.core.usecase.shop.usecases.base.BaseShopUseCase;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseResult;
import com.optivem.shop.testkit.dsl.core.shared.UseCaseContext;
import com.optivem.shop.testkit.dsl.core.shared.VoidVerification;

public class DeliverOrder extends BaseShopUseCase<Void, VoidVerification> {
    private String orderNumberResultAlias;

    public DeliverOrder(ShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public DeliverOrder orderNumber(String orderNumberResultAlias) {
        this.orderNumberResultAlias = orderNumberResultAlias;
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var orderNumber = context.getResultValue(orderNumberResultAlias);
        var result = driver.deliverOrder(orderNumber);
        return new UseCaseResult<>(result, context, VoidVerification::new);
    }
}
