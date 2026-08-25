package com.mycompany.myshop.backend.domain.exceptions;

import com.mycompany.myshop.backend.domain.rules.RuleViolation;

// The transport for a RuleViolation. It carries the violation itself rather than a loose message,
// which is what lets the single catch in RefusalTranslatingUseCase translate by switching over a
// sealed type instead of parsing a string.
//
// Unchecked, deliberately. Checked would put compile-time enforcement behind the one thing this
// design gives up -- a refusal is invisible in the signature -- but it would spread `throws` across
// every method between the rule and the boundary, and it does not survive a lambda, which rules it
// out for a domain whose call sites read `coupon.map(c -> c.discountAt(at))`. The enforcement is
// bought back structurally instead: nothing catches this except the decorator every use case is
// wrapped in, so there is no per-use-case catch left to forget.
public class ValidationException extends RuntimeException {

    private final transient RuleViolation violation;

    public ValidationException(RuleViolation violation) {
        super(violation.message());
        this.violation = violation;
    }

    public ValidationException(String message) {
        this(new RuleViolation.NotAllowed(message));
    }

    public ValidationException(String fieldName, String message) {
        this(new RuleViolation.Malformed(fieldName, message));
    }

    public RuleViolation violation() {
        return violation;
    }

    public String getFieldName() {
        return violation.field();
    }
}
