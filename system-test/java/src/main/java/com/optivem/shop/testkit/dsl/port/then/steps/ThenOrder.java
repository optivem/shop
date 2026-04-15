package com.optivem.shop.testkit.dsl.port.then.steps;

import com.optivem.shop.testkit.driver.port.shop.dtos.OrderStatus;
import com.optivem.shop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrder extends ThenStep<ThenOrder> {
    ThenOrder hasSku(String expectedSku);

    ThenOrder hasQuantity(int expectedQuantity);

    ThenOrder hasUnitPrice(double expectedUnitPrice);

    ThenOrder hasBasePrice(double expectedBasePrice);

    ThenOrder hasBasePrice(String expectedBasePrice);

    ThenOrder hasSubtotalPrice(double expectedSubtotalPrice);

    ThenOrder hasSubtotalPrice(String expectedSubtotalPrice);

    ThenOrder hasTotalPrice(double expectedTotalPrice);

    ThenOrder hasTotalPrice(String expectedTotalPrice);

    ThenOrder hasStatus(OrderStatus expectedStatus);

    ThenOrder hasTotalPriceGreaterThanZero();

    ThenOrder hasOrderNumberPrefix(String expectedPrefix);

    ThenOrder hasDiscountRate(double expectedDiscountRate);

    ThenOrder hasDiscountAmount(double expectedDiscountAmount);

    ThenOrder hasDiscountAmount(String expectedDiscountAmount);

    ThenOrder hasAppliedCouponCode(String expectedCouponCode);

    ThenOrder hasAppliedCoupon(String expectedCouponCode);

    ThenOrder hasAppliedCoupon();

    ThenOrder hasTaxRate(double expectedTaxRate);

    ThenOrder hasTaxRate(String expectedTaxRate);

    ThenOrder hasTaxAmount(String expectedTaxAmount);
}
