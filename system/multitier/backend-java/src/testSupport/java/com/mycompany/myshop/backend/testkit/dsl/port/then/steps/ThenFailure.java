package com.mycompany.myshop.backend.support.port.then.steps;

import com.mycompany.myshop.backend.support.port.then.steps.base.ThenStep;

/**
 * A rejected action, with one expectation per rejection shape. The backend raises validation
 * failures two ways and the DSL keeps them apart, so a test cannot assert the wrong one and pass
 * against a string that verifies nothing:
 *
 * <ul>
 *   <li>{@link #errorMessage(String)} — a whole-request failure (no field), whose message the
 *       handler puts in {@code ProblemDetail.detail}. Example: the New Year blackout.
 *   <li>{@link #fieldErrorMessage(String, String)} — a field-scoped failure, whose {@code detail} is
 *       only the generic "The request contains one or more validation errors" and whose real message
 *       lives in {@code errors[]}. Example: an unknown SKU.
 * </ul>
 *
 * <p>Asserting {@code errorMessage(...)} on a field-scoped failure would compare against that
 * generic string — green, and verifying nothing. That is the trap the split exists to close, and it
 * is why {@code shouldFail()} does not collapse into a single assertion.
 */
public interface ThenFailure extends ThenStep<ThenFailure> {
    ThenFailure errorMessage(String expectedMessage);

    ThenFailure fieldErrorMessage(String expectedField, String expectedMessage);
}
