package com.mycompany.myshop.backend.domain.entities;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

class CouponTest {

    private static final CouponCode CODE = CouponCode.of("SAVE10");
    private static final Rate TEN_PERCENT = Rate.of("0.10");

    private static final Instant VALID_FROM = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant VALID_TO = Instant.parse("2025-12-31T23:59:59Z");
    private static final Instant WITHIN_WINDOW = Instant.parse("2025-06-15T10:00:00Z");
    private static final Instant BEFORE_WINDOW = Instant.parse("2024-12-31T23:59:59Z");
    private static final Instant AFTER_WINDOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void grantsItsDiscountWithinTheValidityWindow() {
        var coupon = coupon(VALID_FROM, VALID_TO, 100, 0);

        assertThat(coupon.discountAt(WITHIN_WINDOW)).isEqualTo(TEN_PERCENT);
    }

    @Test
    void grantsItsDiscountOnBothBoundsOfTheWindow() {
        var coupon = coupon(VALID_FROM, VALID_TO, 100, 0);

        assertThat(coupon.discountAt(VALID_FROM)).isEqualTo(TEN_PERCENT);
        assertThat(coupon.discountAt(VALID_TO)).isEqualTo(TEN_PERCENT);
    }

    @Test
    void rejectsUseBeforeTheWindowOpens() {
        var coupon = coupon(VALID_FROM, VALID_TO, 100, 0);

        var thrown = catchThrowable(() -> coupon.discountAt(BEFORE_WINDOW));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("Coupon code SAVE10 is not yet valid");
        assertThat(((ValidationException) thrown).getFieldName()).isEqualTo("couponCode");
    }

    @Test
    void rejectsUseAfterTheWindowCloses() {
        var coupon = coupon(VALID_FROM, VALID_TO, 100, 0);

        var thrown = catchThrowable(() -> coupon.discountAt(AFTER_WINDOW));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("Coupon code SAVE10 has expired");
        assertThat(((ValidationException) thrown).getFieldName()).isEqualTo("couponCode");
    }

    @Test
    void rejectsUseOnceTheUsageLimitIsReached() {
        var coupon = coupon(VALID_FROM, VALID_TO, 2, 2);

        var thrown = catchThrowable(() -> coupon.discountAt(WITHIN_WINDOW));

        assertThat(thrown).isInstanceOf(ValidationException.class)
                .hasMessage("Coupon code SAVE10 has exceeded its usage limit");
        assertThat(((ValidationException) thrown).getFieldName()).isEqualTo("couponCode");
    }

    @Test
    void treatsAbsentBoundsAsNoRestrictionAtAll() {
        var coupon = coupon(null, null, null, 9_999);

        assertThat(coupon.discountAt(BEFORE_WINDOW)).isEqualTo(TEN_PERCENT);
        assertThat(coupon.discountAt(AFTER_WINDOW)).isEqualTo(TEN_PERCENT);
    }

    @Test
    void redeemRecordsOneMoreUse() {
        var coupon = coupon(VALID_FROM, VALID_TO, 100, 4);

        coupon.redeem();

        assertThat(coupon.getQuota().used()).isEqualTo(5);
    }

    @Test
    void redeemingUpToTheLimitExhaustsTheCoupon() {
        var coupon = coupon(VALID_FROM, VALID_TO, 2, 0);

        coupon.redeem();
        assertThatCode(() -> coupon.discountAt(WITHIN_WINDOW)).doesNotThrowAnyException();

        coupon.redeem();

        assertThat(catchThrowable(() -> coupon.discountAt(WITHIN_WINDOW)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Coupon code SAVE10 has exceeded its usage limit");
    }

    @Test
    void reportsTheWindowBeforeTheUsageLimit() {
        var coupon = coupon(VALID_FROM, VALID_TO, 1, 1);

        assertThat(catchThrowable(() -> coupon.discountAt(BEFORE_WINDOW)))
                .hasMessage("Coupon code SAVE10 is not yet valid");
    }

    // A ValidationException, not an IllegalArgumentException: this rejects a value a caller supplied,
    // so it has to be the type the use case boundary knows how to turn into a reported error. The
    // null checks below stay IllegalArgumentException -- those are programming errors.
    @Test
    void rejectsADiscountRateOutsideZeroToOne() {
        assertThat(catchThrowable(() -> couponWithRate(Rate.ZERO)))
                .isInstanceOf(ValidationException.class)
                .hasMessage("discountRate must be greater than 0 and at most 1");
        assertThat(catchThrowable(() -> couponWithRate(Rate.of("1.01"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("discountRate must be greater than 0 and at most 1");
        assertThatCode(() -> couponWithRate(Rate.ONE)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnyOfItsPartsBeingMissing() {
        assertThat(catchThrowable(() -> new Coupon(null, TEN_PERCENT,
                new ValidityPeriod(VALID_FROM, VALID_TO), UsageQuota.of(100, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("code cannot be null");
        assertThat(catchThrowable(() -> new Coupon(CODE, TEN_PERCENT, null, UsageQuota.of(100, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("validity cannot be null");
        assertThat(catchThrowable(() -> new Coupon(CODE, TEN_PERCENT,
                new ValidityPeriod(VALID_FROM, VALID_TO), null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quota cannot be null");
    }

    private static Coupon coupon(Instant validFrom, Instant validTo, Integer usageLimit,
                                 Integer usedCount) {
        return new Coupon(CODE, TEN_PERCENT, new ValidityPeriod(validFrom, validTo),
                UsageQuota.of(usageLimit, usedCount));
    }

    private static Coupon couponWithRate(Rate discountRate) {
        return new Coupon(CODE, discountRate, new ValidityPeriod(VALID_FROM, VALID_TO),
                UsageQuota.of(100, 0));
    }
}
