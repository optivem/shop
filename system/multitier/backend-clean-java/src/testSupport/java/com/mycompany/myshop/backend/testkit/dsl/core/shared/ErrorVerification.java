package com.mycompany.myshop.backend.testkit.dsl.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

public class ErrorVerification {

    private static final String GENERIC_VALIDATION_DETAIL =
        "The request contains one or more validation errors";

    private final JsonNode problemDetail;

    public ErrorVerification(JsonNode problemDetail) {
        this.problemDetail = problemDetail;
    }

    public ErrorVerification errorMessage(String expectedMessage) {
        assertThat(problemDetail.path("detail").asText())
            .as("ProblemDetail.detail")
            .isEqualTo(expectedMessage);
        return this;
    }

    public ErrorVerification fieldErrorMessage(String expectedField, String expectedMessage) {
        assertThat(problemDetail.path("detail").asText())
            .as("ProblemDetail.detail of a field-scoped failure")
            .isEqualTo(GENERIC_VALIDATION_DETAIL);

        var errors = problemDetail.path("errors");
        assertThat(errors.isArray()).as("ProblemDetail.errors is an array").isTrue();
        assertThat(errors)
            .as("ProblemDetail.errors")
            .anySatisfy(error -> {
                assertThat(error.path("field").asText()).isEqualTo(expectedField);
                assertThat(error.path("message").asText()).isEqualTo(expectedMessage);
            });
        return this;
    }
}
