package com.mycompany.myshop.backend.testkit.dsl.port.then.steps;

import com.mycompany.myshop.backend.domain.values.OrderStatus;
import com.mycompany.myshop.backend.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrder extends ThenStep<ThenOrder> {
    ThenOrder hasSku(String expectedSku);

    ThenOrder hasQuantity(int expectedQuantity);

    ThenOrder hasUnitPrice(String expectedUnitPrice);

    ThenOrder hasUnitPrice(double expectedUnitPrice);

    ThenOrder hasBasePrice(String expectedBasePrice);

    ThenOrder hasBasePrice(double expectedBasePrice);

    ThenOrder hasDiscountRate(String expectedDiscountRate);

    ThenOrder hasDiscountRate(double expectedDiscountRate);

    ThenOrder hasTaxRate(String expectedTaxRate);

    ThenOrder hasTaxRate(double expectedTaxRate);

    ThenOrder hasOrderNumberPrefix(String expectedPrefix);

    ThenOrder hasDiscountAmount(String expectedDiscountAmount);

    ThenOrder hasDiscountAmount(double expectedDiscountAmount);

    ThenOrder hasSubtotalPrice(String expectedSubtotalPrice);

    ThenOrder hasSubtotalPrice(double expectedSubtotalPrice);

    ThenOrder hasTaxAmount(String expectedTaxAmount);

    ThenOrder hasTaxAmount(double expectedTaxAmount);

    ThenOrder hasTotalPrice(String expectedTotalPrice);

    ThenOrder hasTotalPrice(double expectedTotalPrice);

    ThenOrder hasStatus(OrderStatus expectedStatus);

    ThenOrder hasAppliedCoupon(String expectedCouponCode);

    ThenOrder hasAppliedCoupon();

    ThenOrder hasNoAppliedCoupon();
}
