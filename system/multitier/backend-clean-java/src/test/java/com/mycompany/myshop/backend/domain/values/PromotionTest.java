package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The promotion factor, stated directly. {@code OrderPricingTest} and {@code PlaceOrderTest} both
 * feed a factor into the pricing chain, but neither pins down where the factor comes from — that an
 * inactive promotion is a multiply-by-one rather than a skipped step is what keeps the chain
 * branch-free.
 */
class PromotionTest {

    private static final Rate HALF_OFF = Rate.of("0.50");

    @Test
    void appliesItsDiscountWhenActive() {
        assertThat(new Promotion(true, HALF_OFF).factor()).isEqualTo(HALF_OFF);
    }

    /** Inactive leaves the price untouched: the factor is 1, not the discount and not zero. */
    @Test
    void leavesThePriceUntouchedWhenInactive() {
        assertThat(new Promotion(false, HALF_OFF).factor()).isEqualTo(Rate.ONE);
        assertThat(Promotion.inactive().factor()).isEqualTo(Rate.ONE);
    }

    /**
     * Built from ERP wire data, so the guard matters: without it a null discount reaches
     * {@code Money.applyRate} inside the pricing chain and fails there instead of here.
     */
    @Test
    void rejectsConstructionWithoutADiscount() {
        var thrown = catchThrowable(() -> new Promotion(true, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("discount cannot be null");
    }
}
