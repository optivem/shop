package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.rules.RuleViolation;

public sealed interface UseCaseError {

    record NotFound(String entityType, String id) implements UseCaseError { }

    record Invalid(String field, String message) implements UseCaseError { }

    // The one place a domain refusal becomes a use case error. Almost every caller is
    // RefusalTranslatingUseCase, unwrapping a thrown ValidationException; the overload taking a bare
    // RuleViolation is for the one refusal that travels as a value, OrderNumber.parse.
    static UseCaseError from(RuleViolation violation) {
        return new Invalid(violation.field(), violation.message());
    }

    static UseCaseError from(ValidationException exception) {
        return from(exception.violation());
    }
}
