package com.mycompany.myshop.backend.domain.exceptions;

public class NotExistValidationException extends ValidationException {
    public NotExistValidationException(String message) {
        super(message);
    }
}
