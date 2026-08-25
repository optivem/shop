package com.mycompany.myshop.backend.domain.services;

import com.mycompany.myshop.backend.common.Result;
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

    public static void requirePlacementAllowed(Instant at) {
        yearEndTimeOf(at).ifPresent(time -> {
            if (!time.isBefore(PLACEMENT_BLOCKED_FROM)) {
                throw new ValidationException("Orders cannot be placed between 23:59 and 00:00 on December 31st");
            }
        });
    }

    // Returned rather than thrown, because CancelOrder asks this one question on its own and acts on
    // the answer. requirePlacementAllowed above still throws: it is one step inside PlaceOrder's
    // pipeline, where a returned value would have to be unwrapped by every step that follows it.
    public static Result<Void, RuleViolation> cancellationAllowed(Instant at) {
        var time = yearEndTimeOf(at);
        if (time.isPresent()
                && !time.get().isBefore(CANCELLATION_BLOCKED_FROM)
                && !time.get().isAfter(CANCELLATION_BLOCKED_TO)) {
            return Result.err(new RuleViolation.NotAllowed(null,
                    "Order cancellation is not allowed on December 31st between 22:00 and 23:00"));
        }
        return Result.ok(null);
    }

    private static Optional<LocalTime> yearEndTimeOf(Instant at) {
        var local = LocalDateTime.ofInstant(at, ZONE);
        return MonthDay.from(local).equals(YEAR_END)
                ? Optional.of(local.toLocalTime())
                : Optional.empty();
    }
}
