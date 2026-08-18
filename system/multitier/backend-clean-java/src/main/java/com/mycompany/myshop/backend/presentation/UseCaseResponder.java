package com.mycompany.myshop.backend.presentation;

import com.mycompany.myshop.backend.usecases.Result;
import com.mycompany.myshop.backend.usecases.UseCaseError;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Turns a use case's {@link Result} into an HTTP response: the caller supplies what success looks
 * like, this class owns what every failure looks like.
 *
 * <p>The {@code switch} over {@link UseCaseError} has no {@code default} branch, and that is the
 * whole point of the sealed error type — add a case to {@code UseCaseError} and this class stops
 * compiling until it says what that case means over HTTP. The failures used to be reassembled here
 * from exceptions in {@code GlobalExceptionHandler}, which had grown a case per use case and could
 * silently miss a new one.
 */
@Component
public class UseCaseResponder {

    private static final String VALIDATION_DETAIL = "The request contains one or more validation errors";
    private static final String VALIDATION_TITLE = "Validation Error";
    private static final String PROP_TIMESTAMP = "timestamp";
    private static final String PROP_ERRORS = "errors";
    private static final String PROP_FIELD = "field";
    private static final String PROP_MESSAGE = "message";

    @Value("${error.types.validation-error}")
    private String validationErrorTypeUri;

    @Value("${error.types.resource-not-found}")
    private String resourceNotFoundTypeUri;

    /**
     * @param onSuccess what the success value becomes — the status code and headers the contract
     *                  asks for. Called only when the result is {@code Ok}.
     */
    public <T> ResponseEntity<Object> respond(Result<T, UseCaseError> result,
                                              Function<T, ResponseEntity<Object>> onSuccess) {
        if (result.isOk()) {
            return onSuccess.apply(result.value());
        }

        return toProblem(result.error());
    }

    private ResponseEntity<Object> toProblem(UseCaseError error) {
        return switch (error) {
            case UseCaseError.NotFound notFound -> notFound(notFound);
            case UseCaseError.Invalid invalid -> invalid(invalid);
        };
    }

    private ResponseEntity<Object> notFound(UseCaseError.NotFound error) {
        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                error.entityType() + " " + error.id() + " does not exist."
        );
        problemDetail.setType(URI.create(resourceNotFoundTypeUri));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Two renderings, exactly as the exception handler used to produce them: a rule about one named
     * input carries an {@code errors[]} array, a rule about the request as a whole puts its message
     * straight in {@code detail}.
     */
    private ResponseEntity<Object> invalid(UseCaseError.Invalid error) {
        if (error.field() == null) {
            var problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    error.message()
            );
            problemDetail.setType(URI.create(validationErrorTypeUri));
            problemDetail.setTitle(VALIDATION_TITLE);
            problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
        }

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                VALIDATION_DETAIL
        );
        problemDetail.setType(URI.create(validationErrorTypeUri));
        problemDetail.setTitle(VALIDATION_TITLE);
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        var errors = new ArrayList<Map<String, Object>>();
        var errorDetail = new HashMap<String, Object>();
        errorDetail.put(PROP_FIELD, error.field());
        errorDetail.put(PROP_MESSAGE, error.message());
        errors.add(errorDetail);
        problemDetail.setProperty(PROP_ERRORS, errors);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
    }
}
