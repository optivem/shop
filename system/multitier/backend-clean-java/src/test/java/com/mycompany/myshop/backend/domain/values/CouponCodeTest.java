package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CouponCodeTest {

    @Test
    void carriesItsValue() {
        assertThat(CouponCode.of("SAVE10").value()).isEqualTo("SAVE10");
    }

    @Test
    void isEqualToAnotherCodeWithTheSameValue() {
        assertThat(CouponCode.of("SAVE10")).isEqualTo(CouponCode.of("SAVE10"));
        assertThat(CouponCode.of("SAVE10")).isNotEqualTo(CouponCode.of("SAVE20"));
    }

    @Test
    void printsAsItsValue() {
        assertThat(CouponCode.of("SAVE10")).hasToString("SAVE10");
    }

    @Test
    void requiresAValueWhenOneIsMandatory() {
        assertThat(catchThrowable(() -> CouponCode.of(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code cannot be null or empty");
        assertThat(catchThrowable(() -> CouponCode.of("")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code cannot be null or empty");
        assertThat(catchThrowable(() -> CouponCode.of("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code cannot be null or empty");
    }

    @Test
    void readsAnAbsentOrBlankRequestAsNoCouponAsked() {
        assertThat(CouponCode.requested(null)).isEmpty();
        assertThat(CouponCode.requested("")).isEmpty();
        assertThat(CouponCode.requested("   ")).isEmpty();
    }

    @Test
    void readsAPresentRequestAsTheCodeAsked() {
        assertThat(CouponCode.requested("SAVE10")).contains(CouponCode.of("SAVE10"));
    }

    @Test
    void unwrapsAPossiblyAbsentCodeForTheResponseDtos() {
        assertThat(CouponCode.valueOrNull(CouponCode.of("SAVE10"))).isEqualTo("SAVE10");
        assertThat(CouponCode.valueOrNull(null)).isNull();
    }
}
