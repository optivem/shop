package com.mycompany.myshop.backend.presentation.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.mycompany.myshop.backend.domain.gateways.ErpGatewayException;
import com.mycompany.myshop.backend.domain.gateways.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String VALIDATION_DETAIL = "The request contains one or more validation errors";
    private static final String VALIDATION_TITLE = "Validation Error";
    private static final String PROP_TIMESTAMP = "timestamp";
    private static final String GENERAL_ERROR_DETAIL =
            "An unexpected error occurred. Please try again later.";
    private static final String GATEWAY_ERROR_DETAIL =
            "An external system did not answer. Please try again later.";
    private static final String ERP_UNAVAILABLE_DETAIL = "Product information can't be retrieved right now";
    private static final String PROP_ERRORS = "errors";
    private static final String PROP_FIELD = "field";
    private static final String PROP_MESSAGE = "message";

    @Value("${error.types.validation-error}")
    private String validationErrorTypeUri;

    @Value("${error.types.bad-request}")
    private String badRequestTypeUri;

    @Value("${error.types.internal-server-error}")
    private String internalServerErrorTypeUri;

    @Value("${error.types.bad-gateway}")
    private String badGatewayTypeUri;

    @Value("${error.types.service-unavailable}")
    private String serviceUnavailableTypeUri;

    // How long the ERP circuit breaker stays open; the 503 tells the client to retry after it.
    @Value("${resilience.circuit-breaker.wait-in-open-state:10s}")
    private Duration retryAfter;

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

    private Class<?> resolveOwnerClass(Object from) {
        if (from == null) {
            return null;
        }
        return from instanceof Class<?> clazz ? clazz : from.getClass();
    }

    // The ERP is the one external system whose failure is behind a circuit breaker the caller is told
    // about: 503 with Retry-After set to the breaker's open-state wait, so a client knows when asking
    // again can help. Same HTTP contract as backend-java. Never a fallback price -- charging a guessed
    // price is a money bug, so the order is refused instead.
    @ExceptionHandler(ErpGatewayException.class)
    public ResponseEntity<ProblemDetail> handleErpGatewayException(ErpGatewayException ex) {
        // The message names the upstream URL and its response body: logged, never sent.
        log.error("ERP unavailable", ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                ERP_UNAVAILABLE_DETAIL
        );
        problemDetail.setType(URI.create(serviceUnavailableTypeUri));
        problemDetail.setTitle("Service Unavailable");
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, retryAfterSeconds())
                .body(problemDetail);
    }

    // Whole seconds, rounded up and never 0: Retry-After has no finer unit.
    private String retryAfterSeconds() {
        return String.valueOf(Math.max(1, (retryAfter.toMillis() + 999) / 1000));
    }

    // The tax service and the clock: 502 rather than 500. The request was fine and this server is
    // fine, but an upstream system we do not control failed to answer. 502 covers all three ways it
    // can fail us -- unreachable, an error status, a body we could not read. The distinction matters
    // to whoever is paged: a 500 says look at our code, a 502 says look at theirs.
    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<ProblemDetail> handleGatewayException(GatewayException ex) {
        // The message names the upstream URL and its response body. That is exactly what the log needs
        // and exactly what the response must not carry.
        log.error("External system did not answer", ex);

        var problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                GATEWAY_ERROR_DETAIL
        );
        problemDetail.setType(URI.create(badGatewayTypeUri));
        problemDetail.setTitle("Bad Gateway");
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problemDetail);
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
