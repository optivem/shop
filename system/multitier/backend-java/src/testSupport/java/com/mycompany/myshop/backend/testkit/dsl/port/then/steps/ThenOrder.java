package com.mycompany.myshop.backend.support.port.then.steps;

import com.mycompany.myshop.backend.core.entities.OrderStatus;
import com.mycompany.myshop.backend.support.port.then.steps.base.ThenStep;

/** The persisted order, read back through {@code GET /api/orders/{orderNumber}}. */
public interface ThenOrder extends ThenStep<ThenOrder> {
    ThenOrder hasSku(String expectedSku);

    ThenOrder hasQuantity(int expectedQuantity);

    ThenOrder hasUnitPrice(String expectedUnitPrice);

    ThenOrder hasBasePrice(String expectedBasePrice);

    ThenOrder hasDiscountRate(String expectedDiscountRate);

    ThenOrder hasDiscountRate(double expectedDiscountRate);

    ThenOrder hasTaxRate(String expectedTaxRate);

    ThenOrder hasTaxRate(double expectedTaxRate);

    ThenOrder hasOrderNumberPrefix(String expectedPrefix);

    ThenOrder hasDiscountAmount(String expectedDiscountAmount);

    ThenOrder hasSubtotalPrice(String expectedSubtotalPrice);

    ThenOrder hasTaxAmount(String expectedTaxAmount);

    ThenOrder hasTotalPrice(String expectedTotalPrice);

    ThenOrder hasStatus(OrderStatus expectedStatus);

    ThenOrder hasAppliedCoupon(String expectedCouponCode);

    /** Asserts the coupon the action carried was the one applied. */
    ThenOrder hasAppliedCoupon();

    ThenOrder hasNoAppliedCoupon();
}
