package com.mycompany.myshop.backend.presentation.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Framework-level failures only: a body Jackson cannot read, a bean-validation rejection, and the
 * catch-all. Business outcomes are not here — a use case returns those as {@code UseCaseError} and
 * {@code UseCaseResponder} renders them, so they are declared in the signature that produces them
 * instead of being reassembled in a handler that had grown a case per use case.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_DETAIL = "The request contains one or more validation errors";
    private static final String VALIDATION_TITLE = "Validation Error";
    private static final String PROP_TIMESTAMP = "timestamp";
    private static final String GENERAL_ERROR_DETAIL =
            "An unexpected error occurred. Please try again later.";
    private static final String PROP_ERRORS = "errors";
    private static final String PROP_FIELD = "field";
    private static final String PROP_MESSAGE = "message";

    @Value("${error.types.validation-error}")
    private String validationErrorTypeUri;

    @Value("${error.types.bad-request}")
    private String badRequestTypeUri;

    @Value("${error.types.internal-server-error}")
    private String internalServerErrorTypeUri;

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   org.springframework.http.HttpHeaders headers,
                                                                   org.springframework.http.HttpStatusCode status,
                                                                   org.springframework.web.context.request.WebRequest request) {
        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                VALIDATION_DETAIL
        );
        problemDetail.setType(URI.create(validationErrorTypeUri));
        problemDetail.setTitle(VALIDATION_TITLE);
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        var errors = new ArrayList<Map<String, Object>>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            var errorDetail = new HashMap<String, Object>();
            errorDetail.put(PROP_FIELD, ((FieldError) error).getField());
            errorDetail.put(PROP_MESSAGE, error.getDefaultMessage());
            errorDetail.put("code", error.getCode());
            errorDetail.put("rejectedValue", ((FieldError) error).getRejectedValue());
            errors.add(errorDetail);
        });
        problemDetail.setProperty(PROP_ERRORS, errors);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body((Object) problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                   org.springframework.http.HttpHeaders headers,
                                                                   org.springframework.http.HttpStatusCode status,
                                                                   org.springframework.web.context.request.WebRequest request) {
        log.error("HttpMessageNotReadableException: {}", ex.getMessage(), ex);

        var problemDetail = tryBuildTypeMismatchError(ex);
        if (problemDetail != null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body((Object) problemDetail);
        }

        problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Invalid request format"
        );
        problemDetail.setType(URI.create(badRequestTypeUri));
        problemDetail.setTitle("Bad Request");
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body((Object) problemDetail);
    }

    /**
     * A value Jackson could not coerce into the property type is a validation failure about a field
     * we know, not an unreadable body. Both the target class and the property name are read off the
     * exception path itself — no package name is written down here, so moving a request DTO cannot
     * silently disarm this. Returns null when the mismatch cannot be tied to a field carrying
     * {@link com.mycompany.myshop.backend.usecases.TypeValidationMessage}, leaving the 400
     * unreadable-body branch to answer.
     */
    private ProblemDetail tryBuildTypeMismatchError(Throwable ex) {
        var mismatch = findMismatchedInput(ex);
        if (mismatch == null) {
            return null;
        }

        var path = mismatch.getPath();
        if (path == null || path.isEmpty()) {
            return null;
        }

        var reference = path.get(path.size() - 1);
        var fieldName = reference.getFieldName();
        var owner = resolveOwnerClass(reference.getFrom());
        if (fieldName == null || owner == null) {
            return null;
        }

        var fieldMessage = TypeValidationMessageExtractor.extractFieldMessages(owner)
                .get(fieldName.toLowerCase());
        if (fieldMessage == null) {
            return null;
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
        errorDetail.put(PROP_FIELD, fieldName.toLowerCase());
        errorDetail.put(PROP_MESSAGE, fieldMessage);
        errorDetail.put("code", "TYPE_MISMATCH");
        errors.add(errorDetail);
        problemDetail.setProperty(PROP_ERRORS, errors);

        return problemDetail;
    }

    private MismatchedInputException findMismatchedInput(Throwable ex) {
        for (var cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException mismatch) {
                return mismatch;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }
        return null;
    }

    /**
     * {@code Reference.getFrom()} is whatever was being bound when the mismatch happened — usually
     * the partially-built DTO instance, but Jackson may hand back the {@code Class} itself.
     */
    private Class<?> resolveOwnerClass(Object from) {
        if (from == null) {
            return null;
        }
        return from instanceof Class<?> clazz ? clazz : from.getClass();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(Exception ex) {
        // The stack trace carries the message and every cause. None of that goes in the response:
        // an unhandled exception is by definition something we did not mean to expose, and its
        // message routinely names internal classes, SQL, and host addresses.
        log.error("Unexpected error occurred", ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                GENERAL_ERROR_DETAIL
        );
        problemDetail.setType(URI.create(internalServerErrorTypeUri));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
