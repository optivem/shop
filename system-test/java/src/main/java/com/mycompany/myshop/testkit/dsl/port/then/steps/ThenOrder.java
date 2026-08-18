package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrder extends ThenStep<ThenOrder> {
    ThenOrder hasSku(String expectedSku);

    ThenOrder hasQuantity(int expectedQuantity);

    ThenOrder hasUnitPrice(double expectedUnitPrice);

    ThenOrder hasUnitPrice(String expectedUnitPrice);

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

    ThenOrder hasDiscountRate(String expectedDiscountRate);

    ThenOrder hasDiscountAmount(double expectedDiscountAmount);

    ThenOrder hasDiscountAmount(String expectedDiscountAmount);

    ThenOrder hasAppliedCouponCode(String expectedCouponCode);

    ThenOrder hasAppliedCoupon(String expectedCouponCode);

    ThenOrder hasAppliedCoupon();

    ThenOrder hasTaxRate(double expectedTaxRate);

    ThenOrder hasTaxRate(String expectedTaxRate);

    ThenOrder hasTaxAmount(String expectedTaxAmount);

    ThenOrder hasTaxAmount(double expectedTaxAmount);
}
