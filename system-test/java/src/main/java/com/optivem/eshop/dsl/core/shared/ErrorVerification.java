package com.optivem.eshop.dsl.core.shared;

import com.optivem.eshop.dsl.driver.port.shared.dtos.ErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorVerification extends ResponseVerification<ErrorResponse> {
    public ErrorVerification(ErrorResponse error, UseCaseContext context) {
        super(error, context);
    }

    public ErrorVerification errorMessage(String expectedMessage) {
        var expandedExpectedMessage = getContext().expandAliases(expectedMessage);
        var error = getResponse();
        var errorMessage = error.getMessage();
        assertThat(errorMessage)
                .withFailMessage("Expected error message: '%s', but got: '%s'", expandedExpectedMessage, errorMessage)
                .isEqualTo(expandedExpectedMessage);
        return this;
    }

    public ErrorVerification fieldErrorMessage(String expectedField, String expectedMessage) {
        var expandedExpectedField = getContext().expandAliases(expectedField);
        var expandedExpectedMessage = getContext().expandAliases(expectedMessage);
        var error = getResponse();
        var fields = error.getFields();

        assertThat(fields)
                .withFailMessage("Expected field errors but none were found")
                .isNotNull()
                .isNotEmpty();

        var matchingFieldError = fields.stream()
                .filter(f -> expandedExpectedField.equals(f.getField()))
                .findFirst();

        var actualMessage = matchingFieldError
                .map(ErrorResponse.FieldError::getMessage)
                .orElseThrow(() -> new AssertionError(
                        String.format("Expected field error for field '%s', but field was not found in errors: %s",
                                expandedExpectedField, fields)));
        assertThat(actualMessage)
                .withFailMessage("Expected field error message for field '%s': '%s', but got: '%s'",
                        expandedExpectedField, expandedExpectedMessage, actualMessage)
                .isEqualTo(expandedExpectedMessage);

        return this;
    }
}


