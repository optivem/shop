package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The coupon code, and specifically the two readings of a blank one. {@link CouponCode#of} is for
 * callers that require a code — publishing a coupon, reading one back from a row; {@link
 * CouponCode#requested} is for callers to whom "no coupon" is ordinary, which is only placing an
 * order. Getting these the wrong way round is the mistake the type exists to prevent.
 */
class CouponCodeTest {

    @Test
    void carriesItsValue() {
        assertThat(CouponCode.of("SAVE10").value()).isEqualTo("SAVE10");
    }

    /** Compared by value: two codes with the same text are the same code. */
    @Test
    void isEqualToAnotherCodeWithTheSameValue() {
        assertThat(CouponCode.of("SAVE10")).isEqualTo(CouponCode.of("SAVE10"));
        assertThat(CouponCode.of("SAVE10")).isNotEqualTo(CouponCode.of("SAVE20"));
    }

    /** Prints as the bare code, so the "%s" in a coupon's rejection messages reads correctly. */
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
