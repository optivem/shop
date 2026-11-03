package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrder extends ThenStep<ThenOrder> {
    ThenOrder hasSku(String expectedSku);

    ThenOrder hasQuantity(int expectedQuantity);

    ThenOrder hasUnitPrice(String expectedUnitPrice);

    ThenOrder hasBasePrice(String expectedBasePrice);

    ThenOrder hasDiscountRate(String expectedDiscountRate);

    ThenOrder hasTaxRate(String expectedTaxRate);

    ThenOrder hasOrderNumberPrefix(String expectedPrefix);

    ThenOrder hasDiscountAmount(String expectedDiscountAmount);

    ThenOrder hasSubtotalPrice(String expectedSubtotalPrice);

    ThenOrder hasTaxAmount(String expectedTaxAmount);

    ThenOrder hasTotalPrice(String expectedTotalPrice);

    ThenOrder hasStatus(OrderStatus expectedStatus);

    ThenOrder hasAppliedCoupon(String expectedCouponCode);

    ThenOrder hasAppliedCoupon();

    ThenOrder hasNoAppliedCoupon();
}
