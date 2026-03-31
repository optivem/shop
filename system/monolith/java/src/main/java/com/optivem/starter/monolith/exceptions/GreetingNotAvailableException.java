package com.optivem.starter.monolith.exceptions;

public class GreetingNotAvailableException extends RuntimeException {

    public GreetingNotAvailableException(String message) {
        super(message);
    }
}
