package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class OrderPricingTest {

    private static final Rate NO_PROMOTION = Rate.ONE;
    private static final Rate NO_DISCOUNT = Rate.ZERO;
    private static final Rate NO_TAX = Rate.ZERO;

    @Test
    void chargesTheQuantityAtTheUnitPriceWhenNothingIsApplied() {
        var pricing = OrderPricing.price(Money.of("10.00"), 3, NO_PROMOTION, NO_DISCOUNT, NO_TAX);

        assertThat(pricing.basePrice()).isEqualTo(Money.of("30.00"));
        assertThat(pricing.discountAmount()).isEqualTo(Money.ZERO);
        assertThat(pricing.subtotalPrice()).isEqualTo(Money.of("30.00"));
        assertThat(pricing.taxAmount()).isEqualTo(Money.ZERO);
        assertThat(pricing.totalPrice()).isEqualTo(Money.of("30.00"));
    }

    @Test
    void appliesThePromotionThenTheDiscountThenTheTax() {
        var pricing = OrderPricing.price(Money.of("100.00"), 1, Rate.of("0.50"), Rate.of("0.10"),
                Rate.of("0.20"));

        // Discount is 10% of the promoted 50.00, not 10% of the base 100.00.
        assertThat(pricing.discountAmount()).isEqualTo(Money.of("5.00"));
        assertThat(pricing.subtotalPrice()).isEqualTo(Money.of("45.00"));
        // Tax is 20% of the discounted 45.00, not of the promoted 50.00.
        assertThat(pricing.taxAmount()).isEqualTo(Money.of("9.00"));
        assertThat(pricing.totalPrice()).isEqualTo(Money.of("54.00"));
    }

    @Test
    void recordsTheBasePriceBeforeThePromotionIsApplied() {
        var pricing = OrderPricing.price(Money.of("100.00"), 2, Rate.of("0.50"), NO_DISCOUNT, NO_TAX);

        assertThat(pricing.basePrice()).isEqualTo(Money.of("200.00"));
        assertThat(pricing.subtotalPrice()).isEqualTo(Money.of("100.00"));
    }

    @Test
    void roundsEachComponentOnceFromExactIntermediates() {
        var pricing = OrderPricing.price(Money.of("9.99"), 3, Rate.of("0.85"), Rate.of("0.10"),
                Rate.of("0.20"));

        assertThat(pricing.basePrice()).isEqualTo(Money.of("29.97"));
        assertThat(pricing.discountAmount()).isEqualTo(Money.of("2.55"));
        assertThat(pricing.subtotalPrice()).isEqualTo(Money.of("22.93"));
        assertThat(pricing.taxAmount()).isEqualTo(Money.of("4.59"));
        assertThat(pricing.totalPrice()).isEqualTo(Money.of("27.51"));
    }

    @Test
    void recordsComponentsThatNeedNotAddUpToTheRecordedTotal() {
        var pricing = OrderPricing.price(Money.of("9.99"), 3, Rate.of("0.85"), Rate.of("0.10"),
                Rate.of("0.20"));

        var sumOfRecordedParts = pricing.subtotalPrice().plus(pricing.taxAmount());

        assertThat(sumOfRecordedParts).isEqualTo(Money.of("27.52"));
        assertThat(pricing.totalPrice()).isEqualTo(Money.of("27.51"));
    }

    @Test
    void recordsAmountsAtTwoDecimalPlacesAndRatesAtFour() {
        var pricing = OrderPricing.price(Money.of("10"), 1, NO_PROMOTION, Rate.of("0.123456"),
                Rate.of("0.2"));

        assertThat(pricing.unitPrice().amount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(pricing.discountRate().value()).isEqualTo(new BigDecimal("0.1235"));
        assertThat(pricing.taxRate().value()).isEqualTo(new BigDecimal("0.2000"));
    }

    @Test
    void rejectsAMissingComponent() {
        var thrown = catchThrowable(() -> new OrderPricing(null, 1, Money.ZERO, Rate.ZERO,
                Money.ZERO, Money.ZERO, Rate.ZERO, Money.ZERO, Money.ZERO));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("unitPrice cannot be null");
    }
}
