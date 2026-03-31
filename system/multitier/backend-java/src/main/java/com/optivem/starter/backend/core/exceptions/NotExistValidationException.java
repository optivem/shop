package com.optivem.starter.backend.core.exceptions;

public class NotExistValidationException extends ValidationException {
    public NotExistValidationException(String message) {
        super(message);
    }
}
