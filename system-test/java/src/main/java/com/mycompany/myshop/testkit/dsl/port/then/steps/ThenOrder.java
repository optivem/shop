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

    /** The order is the one known in this scenario by the given order-number alias. */
    ThenOrder hasOrderNumber(String orderNumber);

    /** The order is not the one known in this scenario by the given order-number alias. */
    ThenOrder doesNotHaveOrderNumber(String orderNumber);

    ThenOrder hasDiscountRate(String expectedDiscountRate);

    ThenOrder hasDiscountAmount(String expectedDiscountAmount);

    ThenOrder hasAppliedCouponCode(String expectedCouponCode);

    ThenOrder hasAppliedCoupon(String expectedCouponCode);

    ThenOrder hasAppliedCoupon();

    ThenOrder hasTaxRate(String expectedTaxRate);

    ThenOrder hasTaxAmount(String expectedTaxAmount);

    /** The country is resolved like {@link #hasCountry(String)}. */
    ThenOrder hasShippingAddress(String firstName, String lastName, String street, String postalCode,
                                 String city, String country);

    ThenOrder hasNoShippingAddress();

    /** The country as a code, such as "DE", not an alias: checkout offers only real shipping countries. */
    ThenOrder hasCountryCode(String expectedCountry);

    /** The shipping address, with its country as a code, not an alias. */
    ThenOrder hasShippingAddressWithCountryCode(String firstName, String lastName, String street, String postalCode,
                                                String city, String country);
}
