package com.mycompany.myshop.backend.domain.policies;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Both December 31st windows, pinned to the minute. Gathering them in one policy was what made the
 * drift between them visible; this is where the drift is written down so a later reader can see it
 * is deliberate — the placement window runs to midnight, the cancellation window is half an hour
 * long, and its message says 23:00 while its bound is 22:30.
 */
class YearEndBlackoutPolicyTest {

    private static final String PLACEMENT_MESSAGE =
            "Orders cannot be placed between 23:59 and 00:00 on December 31st";
    private static final String CANCELLATION_MESSAGE =
            "Order cancellation is not allowed on December 31st between 22:00 and 23:00";

    @Test
    void allowsPlacementOnAnOrdinaryDay() {
        assertThatCode(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2025-06-15T23:59:59Z")))
                .doesNotThrowAnyException();
    }

    @Test
    void allowsPlacementOnDecember31UpToTheLastMinute() {
        assertThatCode(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2025-12-31T23:58:59Z")))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksPlacementFromTheLastMinuteOfDecember31() {
        assertThat(catchThrowable(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2025-12-31T23:59:00Z"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage(PLACEMENT_MESSAGE);
        assertThat(catchThrowable(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2025-12-31T23:59:59Z"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage(PLACEMENT_MESSAGE);
    }

    @Test
    void allowsCancellationOnAnOrdinaryDay() {
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-06-15T22:15:00Z")))
                .doesNotThrowAnyException();
    }

    /** Both bounds are inclusive: 22:00:00 and 22:30:00 are inside the window. */
    @Test
    void blocksCancellationThroughoutTheHalfHourWindowOnDecember31() {
        assertThat(catchThrowable(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:00:00Z"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage(CANCELLATION_MESSAGE);
        assertThat(catchThrowable(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:15:00Z"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage(CANCELLATION_MESSAGE);
        assertThat(catchThrowable(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:30:00Z"))))
                .isInstanceOf(ValidationException.class)
                .hasMessage(CANCELLATION_MESSAGE);
    }

    @Test
    void allowsCancellationJustBeforeTheWindowOpens() {
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T21:59:59Z")))
                .doesNotThrowAnyException();
    }

    /**
     * The asymmetry, asserted rather than described: the message announces a window that ends at
     * 23:00, but the bound is 22:30, so cancellation at 22:45 on December 31st goes through.
     */
    @Test
    void allowsCancellationAfterTheWindowClosesEvenThoughItsMessageClaims2300() {
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:30:01Z")))
                .doesNotThrowAnyException();
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:45:00Z")))
                .doesNotThrowAnyException();
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:59:59Z")))
                .doesNotThrowAnyException();
    }

    /** Placement and cancellation windows do not overlap: 22:15 blocks one and not the other. */
    @Test
    void appliesTheTwoWindowsIndependently() {
        assertThatCode(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2025-12-31T22:15:00Z")))
                .doesNotThrowAnyException();
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T23:59:00Z")))
                .doesNotThrowAnyException();
    }

    /**
     * The date is read in UTC, not in the caller's zone. 04:59Z on January 1st is 23:59 on
     * December 31st in New York, and is allowed.
     */
    @Test
    void judgesTheDateInUtc() {
        assertThatCode(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2026-01-01T04:59:00Z")))
                .doesNotThrowAnyException();
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2026-01-01T03:15:00Z")))
                .doesNotThrowAnyException();
    }

    private static Instant at(String isoInstant) {
        return Instant.parse(isoInstant);
    }
}
