package com.mycompany.myshop.backend.domain.values;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * The validity window on its own terms. {@code CouponTest} covers what a coupon does with the
 * answers — the reasons it reports, and in what order; these are the answers themselves, including
 * the bounds and the open-ended cases a coupon test would have to construct a whole coupon to reach.
 */
class ValidityPeriodTest {

    private static final Instant FROM = Instant.parse("2025-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2025-12-31T23:59:59Z");
    private static final Instant WITHIN = Instant.parse("2025-06-15T10:00:00Z");
    private static final Instant BEFORE = Instant.parse("2024-12-31T23:59:59Z");
    private static final Instant AFTER = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void isUsableWithinTheWindow() {
        var period = new ValidityPeriod(FROM, TO);

        assertThat(period.notYetValidAt(WITHIN)).isFalse();
        assertThat(period.expiredAt(WITHIN)).isFalse();
    }

    /** Both bounds are inclusive: the opening and closing instants are themselves usable. */
    @Test
    void treatsBothBoundsAsUsable() {
        var period = new ValidityPeriod(FROM, TO);

        assertThat(period.notYetValidAt(FROM)).isFalse();
        assertThat(period.expiredAt(TO)).isFalse();
    }

    @Test
    void isNotYetValidBeforeTheWindowOpens() {
        var period = new ValidityPeriod(FROM, TO);

        assertThat(period.notYetValidAt(BEFORE)).isTrue();
        assertThat(period.expiredAt(BEFORE)).isFalse();
    }

    @Test
    void isExpiredAfterTheWindowCloses() {
        var period = new ValidityPeriod(FROM, TO);

        assertThat(period.expiredAt(AFTER)).isTrue();
        assertThat(period.notYetValidAt(AFTER)).isFalse();
    }

    @Test
    void alwaysIsOpenAtBothEnds() {
        assertThat(ValidityPeriod.ALWAYS.notYetValidAt(BEFORE)).isFalse();
        assertThat(ValidityPeriod.ALWAYS.expiredAt(AFTER)).isFalse();
    }

    /** An absent bound is open-ended in that direction, and the two are independent. */
    @Test
    void treatsAnAbsentBoundAsOpenEndedInThatDirectionOnly() {
        var noStart = new ValidityPeriod(null, TO);
        assertThat(noStart.notYetValidAt(BEFORE)).isFalse();
        assertThat(noStart.expiredAt(AFTER)).isTrue();

        var noEnd = new ValidityPeriod(FROM, null);
        assertThat(noEnd.notYetValidAt(BEFORE)).isTrue();
        assertThat(noEnd.expiredAt(AFTER)).isFalse();
    }

    @Test
    void rejectsAWindowThatClosesBeforeItOpens() {
        var thrown = catchThrowable(() -> new ValidityPeriod(TO, FROM));

        assertThat(thrown).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("validTo must be after validFrom");
    }

    /** A window that opens and closes on the same instant is legal — one usable instant. */
    @Test
    void acceptsAWindowThatOpensAndClosesAtTheSameInstant() {
        assertThatCode(() -> new ValidityPeriod(FROM, FROM)).doesNotThrowAnyException();

        var instantaneous = new ValidityPeriod(FROM, FROM);
        assertThat(instantaneous.notYetValidAt(FROM)).isFalse();
        assertThat(instantaneous.expiredAt(FROM)).isFalse();
    }
}
