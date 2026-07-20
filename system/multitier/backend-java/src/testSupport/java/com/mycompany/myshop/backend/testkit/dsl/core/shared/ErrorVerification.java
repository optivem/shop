package com.mycompany.myshop.backend.support.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A rejected call's RFC 7807 {@code ProblemDetail} body. The backend raises validation failures in
 * two shapes and this exposes one expectation per shape, so a test cannot assert the wrong one:
 *
 * <ul>
 *   <li>{@link #errorMessage(String)} — a whole-request failure (no field), whose message the
 *       handler puts in {@code detail}.
 *   <li>{@link #fieldErrorMessage(String, String)} — a field-scoped failure, whose {@code detail} is
 *       only the generic validation string and whose real message lives in {@code errors[]}.
 * </ul>
 *
 * <p>Asserting {@code detail} on a field-scoped failure would pass against that generic string while
 * verifying nothing — which is exactly the trap the split avoids, so {@link
 * #fieldErrorMessage(String, String)} additionally pins {@code detail} to the generic string a
 * field-scoped failure is supposed to carry.
 */
public class ErrorVerification {

    private static final String GENERIC_VALIDATION_DETAIL =
        "The request contains one or more validation errors";

    private final JsonNode problemDetail;

    public ErrorVerification(JsonNode problemDetail) {
        this.problemDetail = problemDetail;
    }

    /** Asserts the whole-request failure message carried in {@code detail}. */
    public ErrorVerification errorMessage(String expectedMessage) {
        assertThat(problemDetail.path("detail").asText())
            .as("ProblemDetail.detail")
            .isEqualTo(expectedMessage);
        return this;
    }

    /**
     * Asserts {@code errors[]} carries a failure for {@code expectedField} with {@code
     * expectedMessage}, and that {@code detail} is the generic validation string.
     */
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
