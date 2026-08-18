package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;

public sealed interface UseCaseError {

    record NotFound(String entityType, String id) implements UseCaseError { }

    record Invalid(String field, String message) implements UseCaseError { }

    static UseCaseError from(ValidationException exception) {
        return new Invalid(exception.getFieldName(), exception.getMessage());
    }
}
