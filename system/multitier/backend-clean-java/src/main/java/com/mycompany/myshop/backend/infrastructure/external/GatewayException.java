package com.mycompany.myshop.backend.infrastructure.external;

public abstract class GatewayException extends RuntimeException {

    protected GatewayException(String message) {
        super(message);
    }

    protected GatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
