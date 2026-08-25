package com.mycompany.myshop.backend.domain.services;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.rules.RuleViolation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.ZoneId;
import java.util.Optional;

public final class YearEndBlackoutPolicy {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final MonthDay YEAR_END = MonthDay.of(Month.DECEMBER, 31);

    private static final LocalTime PLACEMENT_BLOCKED_FROM = LocalTime.of(23, 59);
    private static final LocalTime CANCELLATION_BLOCKED_FROM = LocalTime.of(22, 0);
    private static final LocalTime CANCELLATION_BLOCKED_TO = LocalTime.of(22, 30);

    private YearEndBlackoutPolicy() {
    }

    // Both windows are asked the same way, and the name says what the caller gets: nothing, or a
    // stop. Neither caller inspects the refusal -- each one only stops -- so neither is made to
    // unwrap a value to discover that.
    public static void requirePlacementAllowed(Instant at) {
        yearEndTimeOf(at).ifPresent(time -> {
            if (!time.isBefore(PLACEMENT_BLOCKED_FROM)) {
                throw new ValidationException("Orders cannot be placed between 23:59 and 00:00 on December 31st");
            }
        });
    }

    public static void requireCancellationAllowed(Instant at) {
        var time = yearEndTimeOf(at);
        if (time.isPresent()
                && !time.get().isBefore(CANCELLATION_BLOCKED_FROM)
                && !time.get().isAfter(CANCELLATION_BLOCKED_TO)) {
            throw new ValidationException(new RuleViolation.NotAllowed(
                    "Order cancellation is not allowed on December 31st between 22:00 and 23:00"));
        }
    }

    private static Optional<LocalTime> yearEndTimeOf(Instant at) {
        var local = LocalDateTime.ofInstant(at, ZONE);
        return MonthDay.from(local).equals(YEAR_END)
                ? Optional.of(local.toLocalTime())
                : Optional.empty();
    }
}
