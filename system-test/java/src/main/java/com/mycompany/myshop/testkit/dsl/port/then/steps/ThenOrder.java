package com.mycompany.myshop.testkit.dsl.port.then.steps;

import com.mycompany.myshop.testkit.common.domain.OrderStatus;
import com.mycompany.myshop.testkit.dsl.port.then.steps.base.ThenStep;

public interface ThenOrder extends ThenStep<ThenOrder> {
    ThenOrder hasSku(String expectedSku);

    ThenOrder hasQuantity(int expectedQuantity);

    ThenOrder hasCountry(String expectedCountry);

    ThenOrder hasUnitPrice(String expectedUnitPrice);

    ThenOrder hasBasePrice(String expectedBasePrice);

    ThenOrder hasSubtotalPrice(String expectedSubtotalPrice);

    ThenOrder hasTotalPrice(String expectedTotalPrice);

    ThenOrder hasStatus(OrderStatus expectedStatus);

    ThenOrder hasTotalPriceGreaterThanZero();

    ThenOrder hasOrderNumberPrefix(String expectedPrefix);

    ThenOrder hasDiscountRate(String expectedDiscountRate);

    ThenOrder hasDiscountAmount(String expectedDiscountAmount);

    ThenOrder hasAppliedCouponCode(String expectedCouponCode);

    ThenOrder hasAppliedCoupon(String expectedCouponCode);

    ThenOrder hasAppliedCoupon();

    ThenOrder hasTaxRate(String expectedTaxRate);

    ThenOrder hasTaxAmount(String expectedTaxAmount);
}
