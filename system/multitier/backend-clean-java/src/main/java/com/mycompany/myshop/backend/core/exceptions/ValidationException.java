package com.mycompany.myshop.backend.core.exceptions;

public class ValidationException extends RuntimeException {
    private final String fieldName;

    public ValidationException(String message) {
        this(null, message);
    }

    public ValidationException(String fieldName, String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
