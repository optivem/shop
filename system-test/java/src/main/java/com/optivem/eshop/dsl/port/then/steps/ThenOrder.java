package com.optivem.eshop.dsl.port.then.steps;

import com.optivem.eshop.dsl.driver.port.shop.dtos.OrderStatus;
import com.optivem.eshop.dsl.port.then.steps.base.ThenStep;

public interface ThenOrder extends ThenStep<ThenOrder> {
    ThenOrder hasSku(String expectedSku);

    ThenOrder hasQuantity(int expectedQuantity);

    ThenOrder hasUnitPrice(double expectedUnitPrice);

    ThenOrder hasTotalPrice(double expectedTotalPrice);

    ThenOrder hasTotalPrice(String expectedTotalPrice);

    ThenOrder hasStatus(OrderStatus expectedStatus);

    ThenOrder hasTotalPriceGreaterThanZero();

    ThenOrder hasOrderNumberPrefix(String expectedPrefix);
}

