package com.mycompany.myshop.backend.domain.services;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowable;

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

    @Test
    void allowsCancellationAfterTheWindowClosesEvenThoughItsMessageClaims2300() {
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:30:01Z")))
                .doesNotThrowAnyException();
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:45:00Z")))
                .doesNotThrowAnyException();
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T22:59:59Z")))
                .doesNotThrowAnyException();
    }

    @Test
    void appliesTheTwoWindowsIndependently() {
        assertThatCode(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2025-12-31T22:15:00Z")))
                .doesNotThrowAnyException();
        assertThatCode(() -> YearEndBlackoutPolicy.requireCancellationAllowed(at("2025-12-31T23:59:00Z")))
                .doesNotThrowAnyException();
    }

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
