package com.optivem.eshop.dsl.driver.port.shared.dtos;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Data
@Builder
public class ErrorResponse {
    private final String message;
    private final List<FieldError> fields;

    public static ErrorResponse of(String message) {
        return ErrorResponse.builder()
                .message(message)
                .build();
    }

    public static ErrorResponse of(String message, FieldError... fieldErrors) {
        return ErrorResponse.builder()
                .message(message)
                .fields(Arrays.asList(fieldErrors))
                .build();
    }

    public static ErrorResponse of(String message, List<FieldError> fieldErrors) {
        return ErrorResponse.builder()
                .message(message)
                .fields(fieldErrors)
                .build();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ErrorResponse{message='").append(message).append("'");

        if (fields != null && !fields.isEmpty()) {
            sb.append(", fieldErrors=[");
            for (int i = 0; i < fields.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(fields.get(i));
            }
            sb.append("]");
        }

        sb.append("}");
        return sb.toString();
    }

    @Getter
    public static class FieldError {
        private final String field;
        private final String message;
        private final String code;

        public FieldError(String field, String message, String code) {
            this.field = field;
            this.message = message;
            this.code = code;
        }

        public FieldError(String field, String message) {
            this(field, message, null);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("FieldError{field='").append(field).append("'");
            sb.append(", message='").append(message).append("'");
            if (code != null) {
                sb.append(", code='").append(code).append("'");
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
