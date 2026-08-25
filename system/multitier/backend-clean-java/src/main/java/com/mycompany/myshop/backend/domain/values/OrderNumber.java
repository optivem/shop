package com.mycompany.myshop.backend.domain.values;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.domain.Guard;
import com.mycompany.myshop.backend.domain.rules.RuleViolation;

import java.util.UUID;

public record OrderNumber(String value) {

    public static final String FIELD_NAME = "orderNumber";

    private static final String PREFIX = "ORD-";

    public OrderNumber {
        Guard.notNullOrEmpty(value, FIELD_NAME);
    }

    public static OrderNumber of(String value) {
        return new OrderNumber(value);
    }

    // The untrusted door, beside `of`. `of` is for values we minted or already stored, where a bad
    // one is a bug and throwing is right; `requested` is for values a caller supplied, where a bad
    // one is an answer. The wording of that answer belongs here rather than in each use case, and
    // callers that would rather report "no such order" than "malformed" still can -- see CancelOrder.
    public static Result<OrderNumber, RuleViolation> requested(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Result.err(new RuleViolation.Malformed(FIELD_NAME, "Order number must not be empty"));
        }
        return Result.ok(new OrderNumber(value));
    }

    public static OrderNumber generate() {
        return new OrderNumber(PREFIX + UUID.randomUUID().toString().toUpperCase());
    }

    @Override
    public String toString() {
        return value;
    }
}
