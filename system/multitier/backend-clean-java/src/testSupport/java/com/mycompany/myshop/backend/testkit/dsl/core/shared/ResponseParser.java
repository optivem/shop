package com.mycompany.myshop.backend.testkit.dsl.core.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;

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
