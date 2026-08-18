package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.domain.exceptions.ValidationException;

/**
 * The closed set of outcomes a use case can report instead of a response.
 *
 * <p>Closed is the point: because this is a {@code sealed interface}, an adapter that maps it
 * switches exhaustively, and adding a case here stops every adapter that does not handle it from
 * compiling. A new exception subclass, by contrast, would just fall through to the catch-all
 * handler and surface as a 500.
 */
public sealed interface UseCaseError {

    /** Something the caller asked for by identity is not there. */
    record NotFound(String entityType, String id) implements UseCaseError { }

    /**
     * The request was understood but breaks a rule. {@code field} is nullable: a rule about one
     * named input carries the field, a rule about the request as a whole does not, and the adapter
     * renders the two differently.
     */
    record Invalid(String field, String message) implements UseCaseError { }

    /**
     * The seam between the domain's exceptions and this layer's values.
     *
     * <p>The domain reports a broken rule by throwing — {@code Order.cancel()} on an already
     * cancelled order, {@code YearEndBlackoutPolicy} during the blackout window. That is the
     * domain's business and this layer does not change it; it translates at its own boundary, so
     * the exception stops here rather than travelling to a handler three layers away.
     *
     * <p>Only {@link ValidationException} crosses. An {@code IllegalArgumentException} from a
     * {@code Guard} is a programming error, not a business outcome, and stays an exception.
     */
    static UseCaseError from(ValidationException exception) {
        return new Invalid(exception.getFieldName(), exception.getMessage());
    }
}
