package com.mycompany.myshop.backend.infrastructure.external;

public class ClockGatewayException extends GatewayException {

    public ClockGatewayException(String message) {
        super(message);
    }

    public ClockGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
