package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.rules.RuleViolation;

public sealed interface UseCaseError {

    record NotFound(String entityType, String id) implements UseCaseError { }

    record Invalid(String field, String message) implements UseCaseError { }

    // The one place a domain refusal becomes a use case error, whichever way it travelled: returned
    // as a value by a single-decision rule, or thrown as a short-circuit out of a pipeline.
    static UseCaseError from(RuleViolation violation) {
        return new Invalid(violation.field(), violation.message());
    }

    static UseCaseError from(ValidationException exception) {
        return from(exception.violation());
    }
}
