package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PromotionTest {

    private static final Rate HALF_OFF = Rate.of("0.50");

    @Test
    void appliesItsDiscountWhenActive() {
        assertThat(new Promotion(true, HALF_OFF).factor()).isEqualTo(HALF_OFF);
    }

    @Test
    void leavesThePriceUntouchedWhenInactive() {
        assertThat(new Promotion(false, HALF_OFF).factor()).isEqualTo(Rate.ONE);
        assertThat(Promotion.inactive().factor()).isEqualTo(Rate.ONE);
    }

    @Test
    void rejectsConstructionWithoutADiscount() {
        var thrown = catchThrowable(() -> new Promotion(true, null));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("discount cannot be null");
    }
}
