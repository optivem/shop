package com.optivem.shop.monolith.api.exception;

import com.optivem.shop.monolith.core.exceptions.NotExistValidationException;
import com.optivem.shop.monolith.core.exceptions.ValidationException;
import com.optivem.shop.monolith.core.validation.TypeValidationMessageExtractor;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile(
            "(com\\.optivem\\.shop\\.monolith\\.core\\.dtos\\.[^\\[\\]\"\\s\\)]+)");

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

    @Value("${error.types.bad-request}")
    private String badRequestTypeUri;

    @Value("${error.types.internal-server-error}")
    private String internalServerErrorTypeUri;

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(ValidationException ex) {
        if (ex.getFieldName() != null) {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    VALIDATION_DETAIL
            );
            problemDetail.setType(URI.create(validationErrorTypeUri));
            problemDetail.setTitle(VALIDATION_TITLE);
            problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

            List<Map<String, Object>> errors = new ArrayList<>();
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put(PROP_FIELD, ex.getFieldName());
            errorDetail.put(PROP_MESSAGE, ex.getMessage());
            errors.add(errorDetail);
            problemDetail.setProperty(PROP_ERRORS, errors);

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
        } else {
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    ex.getMessage()
            );
            problemDetail.setType(URI.create(validationErrorTypeUri));
            problemDetail.setTitle(VALIDATION_TITLE);
            problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problemDetail);
        }
    }

    @ExceptionHandler(NotExistValidationException.class)
    public ResponseEntity<ProblemDetail> handleNotExistValidationException(NotExistValidationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setType(URI.create(resourceNotFoundTypeUri));
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                   org.springframework.http.HttpHeaders headers,
                                                                   org.springframework.http.HttpStatusCode status,
                                                                   org.springframework.web.context.request.WebRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                VALIDATION_DETAIL
        );
        problemDetail.setType(URI.create(validationErrorTypeUri));
        problemDetail.setTitle(VALIDATION_TITLE);
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        List<Map<String, Object>> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            Map<String, Object> errorDetail = new HashMap<>();
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

        ProblemDetail problemDetail = tryParseFieldError(ex.getMessage());
        if (problemDetail != null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body((Object) problemDetail);
        }

        if (ex.getCause() != null) {
            problemDetail = tryParseFieldError(ex.getCause().getMessage());
            if (problemDetail != null) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body((Object) problemDetail);
            }
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

    private ProblemDetail tryParseFieldError(String message) {
        if (message == null) {
            return null;
        }

        Class<?> dtoClass = extractDtoClass(message);
        if (dtoClass == null) {
            return null;
        }

        Map<String, String> fieldErrorPatterns = TypeValidationMessageExtractor.extractFieldMessages(dtoClass);
        String lowerMessage = message.toLowerCase();

        return fieldErrorPatterns.entrySet().stream()
                .filter(entry -> lowerMessage.contains(entry.getKey()))
                .findFirst()
                .map(entry -> {
                    String fieldName = entry.getKey();
                    String fieldMessage = entry.getValue();

                    ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                            HttpStatus.UNPROCESSABLE_ENTITY,
                            VALIDATION_DETAIL
                    );
                    pd.setType(URI.create(validationErrorTypeUri));
                    pd.setTitle(VALIDATION_TITLE);
                    pd.setProperty(PROP_TIMESTAMP, Instant.now());

                    List<Map<String, Object>> errors = new ArrayList<>();
                    Map<String, Object> errorDetail = new HashMap<>();
                    errorDetail.put(PROP_FIELD, fieldName);
                    errorDetail.put(PROP_MESSAGE, fieldMessage);
                    errorDetail.put("code", "TYPE_MISMATCH");
                    errors.add(errorDetail);
                    pd.setProperty(PROP_ERRORS, errors);

                    return pd;
                })
                .orElse(null);
    }

    private Class<?> extractDtoClass(String message) {
        Matcher matcher = CLASS_NAME_PATTERN.matcher(message);
        if (matcher.find()) {
            String className = matcher.group(1);
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                log.debug("Could not load class: {}", className);
            }
        }
        return null;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneralException(Exception ex) {
        log.error("Unexpected error occurred", ex);

        String rootCauseMessage = getRootCauseMessage(ex);
        String fullMessage = "Internal server error: " + ex.getMessage();
        if (rootCauseMessage != null && !rootCauseMessage.equals(ex.getMessage())) {
            fullMessage += " | Root cause: " + rootCauseMessage;
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                fullMessage
        );
        problemDetail.setType(URI.create(internalServerErrorTypeUri));
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty(PROP_TIMESTAMP, Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    private String getRootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
