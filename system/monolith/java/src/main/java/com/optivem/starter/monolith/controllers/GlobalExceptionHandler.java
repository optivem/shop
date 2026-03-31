package com.optivem.starter.monolith.controllers;

import com.optivem.starter.monolith.exceptions.GreetingNotAvailableException;
import com.optivem.starter.monolith.models.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GreetingNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleGreetingNotAvailable(
            GreetingNotAvailableException ex) {
        ErrorResponse error = new ErrorResponse("GREETING_NOT_AVAILABLE", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
