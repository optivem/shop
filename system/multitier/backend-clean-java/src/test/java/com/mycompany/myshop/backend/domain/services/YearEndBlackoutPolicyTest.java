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
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-06-15T22:15:00Z")).isOk()).isTrue();
    }

    @Test
    void blocksCancellationThroughoutTheHalfHourWindowOnDecember31() {
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T22:00:00Z")).error().message()).isEqualTo(CANCELLATION_MESSAGE);
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T22:15:00Z")).error().message()).isEqualTo(CANCELLATION_MESSAGE);
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T22:30:00Z")).error().message()).isEqualTo(CANCELLATION_MESSAGE);
    }

    @Test
    void allowsCancellationJustBeforeTheWindowOpens() {
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T21:59:59Z")).isOk()).isTrue();
    }

    @Test
    void allowsCancellationAfterTheWindowClosesEvenThoughItsMessageClaims2300() {
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T22:30:01Z")).isOk()).isTrue();
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T22:45:00Z")).isOk()).isTrue();
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T22:59:59Z")).isOk()).isTrue();
    }

    @Test
    void appliesTheTwoWindowsIndependently() {
        assertThatCode(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2025-12-31T22:15:00Z")))
                .doesNotThrowAnyException();
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2025-12-31T23:59:00Z")).isOk()).isTrue();
    }

    @Test
    void judgesTheDateInUtc() {
        assertThatCode(() -> YearEndBlackoutPolicy.requirePlacementAllowed(at("2026-01-01T04:59:00Z")))
                .doesNotThrowAnyException();
        assertThat(YearEndBlackoutPolicy.cancellationAllowed(at("2026-01-01T03:15:00Z")).isOk()).isTrue();
    }

    private static Instant at(String isoInstant) {
        return Instant.parse(isoInstant);
    }
}
