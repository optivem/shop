package com.mycompany.myshop.backend.support.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;

/**
 * Parses a raw response body into either the success DTO or the RFC 7807 {@code ProblemDetail},
 * once the test has said which outcome it expects.
 *
 * <p>The endpoints that can answer either way — place order, view order — are fetched as raw {@code
 * String} by {@code BackendDriver}: binding them to the success type would discard the {@code
 * ProblemDetail} body on a 4xx (Jackson would quietly map it onto a null-filled success DTO),
 * leaving nothing to assert a rejection message against.
 */
public final class ResponseParser {

    private ResponseParser() {
    }

    public static <T> T parseSuccess(
            ResponseEntity<String> response, Class<T> type, ObjectMapper objectMapper) {
        assertThat(response.getBody()).as("success body").isNotNull();
        return parse(response.getBody(), type, objectMapper);
    }

    public static ErrorVerification parseRejection(
            ResponseEntity<String> response, ObjectMapper objectMapper) {
        assertThat(response.getBody()).as("rejection body").isNotNull();
        return new ErrorVerification(parse(response.getBody(), JsonNode.class, objectMapper));
    }

    private static <T> T parse(String body, Class<T> type, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(body, type);
        } catch (Exception e) {
            throw new AssertionError(
                "Could not parse response body as " + type.getSimpleName() + ": " + body, e);
        }
    }
}
