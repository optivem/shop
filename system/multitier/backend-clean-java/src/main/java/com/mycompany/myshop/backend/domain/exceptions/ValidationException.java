package com.mycompany.myshop.backend.domain.exceptions;

import com.mycompany.myshop.backend.domain.rules.RuleViolation;

// The thrown transport for a RuleViolation. It carries the violation itself rather than a loose
// message, so a use case that catches one and a use case that receives one as a return value both
// end at the same UseCaseError.from(...) -- one vocabulary, one translation, two ways of travelling.
public class ValidationException extends RuntimeException {

    private final transient RuleViolation violation;

    public ValidationException(RuleViolation violation) {
        super(violation.message());
        this.violation = violation;
    }

    public ValidationException(String message) {
        this(new RuleViolation.NotAllowed(null, message));
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
